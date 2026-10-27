package com.chathala.hala.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class TokenStorageTest {
    private val directory = Files.createTempDirectory("hala-session-test").toFile()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storage = TokenStorage(PreferenceDataStoreFactory.create(scope = scope) {
        directory.resolve("tokens.preferences_pb")
    })

    @After fun close() = runBlocking {
        scope.coroutineContext[Job]!!.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }

    @Test fun `login without refresh token removes previous accounts credentials`() = runBlocking {
        val first = storage.save("account-A", "refresh-A")
        val second = storage.save("account-B", null)
        assertNotEquals(first.sessionId, second.sessionId)
        assertNull(storage.snapshot()?.refreshToken)
        assertEquals("account-B", storage.snapshot()?.accessToken)
    }

    @Test fun `stale refresh cannot overwrite or clear a new login`() = runBlocking {
        val first = storage.save("account-A", "refresh-A")
        val second = storage.save("account-B", "refresh-B")
        assertFalse(storage.replaceIfCurrent(first, "renewed-A", "rotated-A"))
        assertFalse(storage.clearIfCurrent(first))
        assertEquals(second, storage.snapshot())
    }

    @Test fun `logout waits for an in-flight guarded cache write before cleaning`() = runBlocking {
        val session = storage.save("account-A", "refresh-A")
        val writeStarted = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        var cache: String? = null
        storage.onSessionCleared = { cache = null }
        val writer = launch(Dispatchers.Default) {
            storage.withSession(session.sessionId) {
                writeStarted.complete(Unit)
                finishWrite.await()
                cache = "private account A messages"
            }
        }
        withTimeout(5_000) { writeStarted.await() }
        val logout = launch { storage.clear() }
        finishWrite.complete(Unit)
        withTimeout(5_000) { joinAll(writer, logout) }
        assertNull(cache)
        assertNull(storage.snapshot())
        assertNull(storage.withSession(session.sessionId) { cache = "late write"; true })
        assertNull(cache)
    }

    @Test fun `cleanup runs on invalidation and token rotation retains session identity`() = runBlocking {
        val session = storage.save("account-A", "refresh-A")
        var cleaned = false
        storage.onSessionCleared = { cleaned = true }
        assertTrue(storage.replaceIfCurrent(session, "new-access", null))
        assertFalse(cleaned)
        val rotated = storage.snapshot()!!
        assertEquals(session.sessionId, rotated.sessionId)
        assertEquals("refresh-A", rotated.refreshToken)
        assertTrue(storage.clearIfCurrent(rotated))
        assertTrue(cleaned)
        assertNull(storage.snapshot())
    }
    @Test fun `logout follows rotation but cannot sign out a later login`() = runBlocking {
        val session = storage.save("account-A", "refresh-A")
        storage.replaceIfCurrent(session, "rotated-access", null)
        assertTrue(storage.endSession(session.sessionId))
        val second = storage.save("account-B", "refresh-B")
        assertFalse(storage.endSession(session.sessionId))
        assertEquals(second, storage.snapshot())
    }

}
