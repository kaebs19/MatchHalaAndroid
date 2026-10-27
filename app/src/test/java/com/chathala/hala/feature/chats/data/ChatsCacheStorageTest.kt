package com.chathala.hala.feature.chats.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chathala.hala.core.storage.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class ChatsCacheStorageTest {
    private val directory = Files.createTempDirectory("hala-cache-test").toFile()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private fun store(name: String) = PreferenceDataStoreFactory.create(scope = scope) {
        directory.resolve("$name.preferences_pb")
    }
    private val tokens = TokenStorage(store("session"))
    private val cache = ChatsCacheStorage(store("chats"), Moshi.Builder().add(KotlinJsonAdapterFactory()).build(), tokens)

    @After fun close() = runBlocking {
        scope.coroutineContext[Job]!!.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }

    @Test fun `late response from previous account cannot repopulate cache`() = runBlocking {
        val first = tokens.save("A", "refresh-A")
        tokens.onSessionCleared = { cache.clear() }
        cache.saveConversations(listOf(Conversation(id = "private-A")), first.sessionId)
        val second = tokens.save("B", "refresh-B")
        cache.saveConversations(listOf(Conversation(id = "late-A")), first.sessionId)
        cache.saveMessages("chat-A", listOf(Message(id = "private-message-A")), first.sessionId)
        assertNull(cache.readConversations(second.sessionId))
        assertNull(cache.readMessages("chat-A", second.sessionId))
    }

    @Test fun `old viewmodel cannot read the new accounts cache`() = runBlocking {
        val first = tokens.save("A", "refresh-A")
        val second = tokens.save("B", "refresh-B")
        cache.saveConversations(listOf(Conversation(id = "private-B")), second.sessionId)
        assertNull(cache.readConversations(first.sessionId))
        assertEquals("private-B", cache.readConversations(second.sessionId)?.single()?.id)
    }

    @Test fun `temporary and disappearing messages are excluded from persisted history`() = runBlocking {
        val session = tokens.save("A", "refresh-A")
        cache.saveMessages("chat", listOf(
            Message(id = "permanent"),
            Message(id = "tmp-sending"),
            Message(id = "disappearing", mediaUrl = "https://example.test/private", disappearing = DisappearingInfo(enabled = true))
        ), session.sessionId)
        assertEquals(listOf("permanent"), cache.readMessages("chat", session.sessionId)?.map { it.id })
    }

    @Test fun `cache retains the latest hundred persistent messages`() = runBlocking {
        val session = tokens.save("A", "refresh-A")
        cache.saveMessages("chat", (1..150).map { Message(id = it.toString()) }, session.sessionId)
        val result = cache.readMessages("chat", session.sessionId)!!
        assertEquals(100, result.size)
        assertEquals("51", result.first().id)
        assertEquals("150", result.last().id)
    }
}
