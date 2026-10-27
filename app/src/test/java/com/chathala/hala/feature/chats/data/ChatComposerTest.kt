package com.chathala.hala.feature.chats.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class ChatComposerTest {
    private val directory = Files.createTempDirectory("hala-composer-test").toFile()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private fun dataStore(name: String) = PreferenceDataStoreFactory.create(scope = scope) { directory.resolve("$name.preferences_pb") }
    private val tokens = TokenStorage(dataStore("session"))
    private val preferences = dataStore("composer")
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val storage = ChatComposerStorage(preferences, moshi, tokens, scope)
    private var released = 0
    private val attachments = object : OutgoingAttachments {
        override suspend fun prepare(entry: OutgoingMessage): OutgoingMessage = entry
        override suspend fun release(entry: OutgoingMessage) { released++; entry.attachmentPath?.let { File(it).delete() } }
    }
    private fun coordinator(sender: suspend (OutgoingMessage) -> OutgoingSendResult) = ChatSendCoordinator(
        storage, tokens, scope, attachments, { "interrupted" }, { "attachment missing" }
    ) { _, entry -> sender(entry) }
    private fun text(id: String = "tmp-one", status: OutgoingStatus = OutgoingStatus.PREPARING) = OutgoingMessage(
        localId = id, message = Message(id = id, conversation = "chat", content = "hello", type = "text"), status = status
    )
    private suspend fun Job?.finish() { withTimeout(5_000) { requireNotNull(this@finish).join() } }
    private suspend fun entry(session: String) = storage.readOutgoing(session, "chat").single()

    @After fun close() = runBlocking {
        scope.coroutineContext[Job]!!.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }

    @Test fun `draft text and reply survive a new storage reader and stay per conversation`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        val draft = ChatDraft("مرحبا", MessageReply(id = "reply", content = "question", type = "text", sender = MessageReplySender("Sara")))
        storage.saveDraft(session, "chat", draft)
        storage.saveDraft(session, "other", ChatDraft("different"))
        val reopened = ChatComposerStorage(preferences, moshi, tokens, scope)
        assertEquals(draft, reopened.readDraft(session, "chat"))
        assertEquals("different", reopened.readDraft(session, "other")?.text)
    }

    @Test fun `clearing text and reply removes the stored draft`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        storage.saveDraft(session, "chat", ChatDraft("old"))
        storage.saveDraft(session, "chat", ChatDraft())
        assertNull(storage.readDraft(session, "chat"))
    }

    @Test fun `queued typing preserves order and survives the caller leaving`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        var finalWrite: Job? = null
        val caller = launch {
            storage.queueDraft(session, "chat", ChatDraft("first"))
            finalWrite = storage.queueDraft(session, "chat", ChatDraft("last keystroke"))
        }
        caller.join()
        caller.cancel()
        finalWrite.finish()
        assertEquals("last keystroke", storage.readDraft(session, "chat")?.text)
    }

    @Test fun `durable outbox consumes only the matching draft`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        val draft = ChatDraft("hello")
        storage.saveDraft(session, "chat", draft)
        storage.put(session, "chat", text(), draft)
        assertNull(storage.readDraft(session, "chat"))
        assertEquals("hello", entry(session).message.content)
        storage.saveDraft(session, "chat", ChatDraft("new typing"))
        storage.put(session, "chat", text("tmp-second"), draft)
        assertEquals("new typing", storage.readDraft(session, "chat")?.text)
    }

    @Test fun `logout and a late response cannot expose drafts or failed messages to another login`() = runBlocking {
        val first = tokens.save("A", "refresh-A").sessionId
        tokens.onSessionCleared = { storage.clear() }
        storage.saveDraft(first, "chat", ChatDraft("private"))
        storage.put(first, "chat", text(status = OutgoingStatus.FAILED))
        val second = tokens.save("B", "refresh-B").sessionId
        storage.queueDraft(first, "chat", ChatDraft("late")).finish()
        assertFalse(storage.put(first, "chat", text("tmp-late")))
        assertNull(storage.readDraft(second, "chat"))
        assertTrue(storage.readOutgoing(second, "chat").isEmpty())
    }

    @Test fun `interrupted sends become manual failures without sending automatically`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        storage.put(session, "chat", text(status = OutgoingStatus.SENDING))
        val count = AtomicInteger()
        val coordinator = coordinator { count.incrementAndGet(); error("must not auto-send") }
        coordinator.restore(session, "chat")
        assertEquals(OutgoingStatus.FAILED, entry(session).status)
        assertTrue(entry(session).deliveryUncertain)
        assertEquals(0, count.get())
    }

    @Test fun `active sends are not marked failed when the chat is reopened`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val coordinator = coordinator {
            started.complete(Unit); finish.await()
            OutgoingSendResult(NetworkResult.Success(it.message.copy(id = "server-id")))
        }
        val job = coordinator.submit(session, "chat", text())
        withTimeout(5_000) { started.await() }
        coordinator.restore(session, "chat")
        assertEquals(OutgoingStatus.SENDING, entry(session).status)
        finish.complete(Unit); job.finish()
        assertEquals(OutgoingStatus.SENT, entry(session).status)
    }

    @Test fun `failed voice keeps its file waveform and duration until retry succeeds`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        val file = directory.resolve("voice.m4a").apply { writeText("audio") }
        val voice = text().copy(message = text().message.copy(type = "audio", content = null, mediaUrl = file.path, audioDuration = 4.0, audioWaveform = listOf(0.1, 0.9)), attachmentPath = file.path)
        val count = AtomicInteger()
        val coordinator = coordinator {
            assertEquals(4.0, it.message.audioDuration!!, 0.0)
            assertEquals(listOf(0.1, 0.9), it.message.audioWaveform)
            if (count.incrementAndGet() == 1) OutgoingSendResult(NetworkResult.Error("offline"))
            else OutgoingSendResult(NetworkResult.Success(it.message.copy(id = "real-voice", mediaUrl = "https://example.test/audio")))
        }
        coordinator.submit(session, "chat", voice).finish()
        assertTrue(file.exists())
        assertEquals(OutgoingStatus.FAILED, entry(session).status)
        assertEquals(0, released)
        assertNull(coordinator.retry(session, "chat", voice.localId))
        coordinator.retry(session, "chat", voice.localId, confirmed = true).finish()
        assertEquals(2, count.get())
        assertEquals(OutgoingStatus.SENT, entry(session).status)
        assertFalse(file.exists())
        assertEquals(1, released)
    }

    @Test fun `blocked messages never retry even after confirmation`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        val count = AtomicInteger()
        val coordinator = coordinator {
            count.incrementAndGet()
            OutgoingSendResult(NetworkResult.Error("blocked", code = "USER_BLOCKED", httpCode = 403))
        }
        coordinator.submit(session, "chat", text()).finish()
        assertEquals(OutgoingStatus.REJECTED, entry(session).status)
        assertFalse(entry(session).retryAllowed)
        assertNull(coordinator.retry(session, "chat", "tmp-one", confirmed = true))
        assertEquals(1, count.get())
    }

    @Test fun `double retry while uploading starts exactly one network request`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        storage.put(session, "chat", text(status = OutgoingStatus.FAILED))
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val count = AtomicInteger()
        val coordinator = coordinator {
            count.incrementAndGet(); started.complete(Unit); finish.await()
            OutgoingSendResult(NetworkResult.Success(it.message.copy(id = "server-id")))
        }
        val first = coordinator.retry(session, "chat", "tmp-one")
        withTimeout(5_000) { started.await() }
        assertNull(coordinator.retry(session, "chat", "tmp-one"))
        finish.complete(Unit); first.finish()
        assertEquals(1, count.get())
        assertEquals(1, storage.readOutgoing(session, "chat").size)
        assertNull(coordinator.retry(session, "chat", "tmp-one"))
    }

    @Test fun `discarding a failed attachment removes the local record and file`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        val file = directory.resolve("failed.jpg").apply { writeText("image") }
        storage.put(session, "chat", text(status = OutgoingStatus.FAILED).copy(attachmentPath = file.path))
        coordinator { error("must not send") }.discard(session, "chat", "tmp-one")
        assertTrue(storage.readOutgoing(session, "chat").isEmpty())
        assertFalse(file.exists())
    }

    @Test fun `late send completion cannot write under a different session`() = runBlocking {
        val first = tokens.save("A", "refresh-A").sessionId
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val coordinator = coordinator {
            started.complete(Unit); finish.await()
            OutgoingSendResult(NetworkResult.Success(it.message.copy(id = "real")))
        }
        val job = coordinator.submit(first, "chat", text())
        withTimeout(5_000) { started.await() }
        tokens.onSessionCleared = { storage.clear() }
        val second = tokens.save("B", "refresh-B").sessionId
        finish.complete(Unit); job.finish()
        assertTrue(storage.readOutgoing(second, "chat").isEmpty())
    }

    @Test fun `server rejection takes precedence over retryable http status`() {
        assertFalse(NetworkResult.Error("rejected", code = "CONVERSATION_CANCELLED", httpCode = 503).permitsMessageRetry())
        assertTrue(NetworkResult.Error("rate limit", httpCode = 429).permitsMessageRetry())
        assertFalse(NetworkResult.Error("rate limit", httpCode = 429).hasUncertainDelivery())
        assertTrue(NetworkResult.Error("timeout", httpCode = 504).hasUncertainDelivery())
        assertTrue(NetworkResult.Error("unavailable", code = "SERVER_ERROR", httpCode = 503).permitsMessageRetry())
        assertFalse(NetworkResult.Error("forbidden", httpCode = 403).permitsMessageRetry())
    }

    @Test fun `stale retry cannot overwrite a completed outbox record`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        storage.put(session, "chat", text(status = OutgoingStatus.SENT))
        assertFalse(storage.put(session, "chat", text(status = OutgoingStatus.PREPARING), requireFailed = true))
        assertEquals(OutgoingStatus.SENT, entry(session).status)
    }
    @Test fun `switching owner clears unrelated conversation keys even without a cleanup hook`() = runBlocking {
        val first = tokens.save("A", "refresh-A").sessionId
        storage.saveDraft(first, "private-chat", ChatDraft("private A"))
        storage.put(first, "private-chat", text(status = OutgoingStatus.FAILED))
        val second = tokens.save("B", "refresh-B").sessionId
        storage.saveDraft(second, "new-chat", ChatDraft("B"))
        assertNull(storage.readDraft(second, "private-chat"))
        assertTrue(storage.readOutgoing(second, "private-chat").isEmpty())
    }

    @Test fun `deleting a conversation clears its draft failed uploads and files only`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        val file = directory.resolve("voice-delete.m4a").apply { writeText("audio") }
        storage.saveDraft(session, "chat", ChatDraft("remove"))
        storage.saveDraft(session, "other", ChatDraft("keep"))
        storage.put(session, "chat", text(status = OutgoingStatus.FAILED).copy(attachmentPath = file.path))
        coordinator { error("must not send") }.clearConversation(session, "chat")
        assertNull(storage.readDraft(session, "chat"))
        assertTrue(storage.readOutgoing(session, "chat").isEmpty())
        assertFalse(file.exists())
        assertEquals("keep", storage.readDraft(session, "other")?.text)
    }

    @Test fun `unprepared attachment interrupted by process death needs a fresh selection`() = runBlocking {
        val session = tokens.save("A", "refresh").sessionId
        storage.put(session, "chat", text().copy(message = text().message.copy(type = "image", mediaUrl = "content://old-selection")))
        coordinator { error("must not send") }.restore(session, "chat")
        assertEquals(OutgoingStatus.REJECTED, entry(session).status)
        assertFalse(entry(session).retryAllowed)
        assertFalse(entry(session).deliveryUncertain)
    }

}
