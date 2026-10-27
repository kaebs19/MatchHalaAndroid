package com.chathala.hala.feature.chats.data

import org.junit.Assert.*
import org.junit.Test

class MessageSyncTest {
    private fun message(id: String, second: Int = 0) = Message(id = id, content = id, createdAt = "2026-10-18T10:00:${second.toString().padStart(2, '0')}Z")

    @Test fun `incoming message survives an older HTTP snapshot`() {
        val old = message("old")
        val incoming = message("incoming", 1)
        assertEquals(listOf(old, incoming), mergeFetchedMessages(listOf(old), listOf(old), listOf(old, incoming)))
    }

    @Test fun `socket deletion and edit win over a stale HTTP response`() {
        val original = listOf(message("edited"), message("deleted", 1))
        val current = listOf(original[0].copy(content = "new", isEdited = true), original[1].copy(content = "", isDeleted = true))
        assertEquals(current, mergeFetchedMessages(original, original, current))
    }

    @Test fun `unchanged cached values do not override the server`() {
        val cached = message("one")
        val server = cached.copy(isRead = true)
        assertEquals(listOf(server), mergeFetchedMessages(listOf(server), listOf(cached), listOf(cached)))
    }

    @Test fun `deduplicates socket and HTTP copies and preserves pending send`() {
        val incoming = message("incoming", 1)
        val pending = message("tmp-pending", 2)
        val result = mergeFetchedMessages(listOf(incoming), emptyList(), listOf(incoming, pending))
        assertEquals(listOf(incoming, pending), result)
    }

    @Test fun `server disappearance deadline parses offsets and rejects invalid values`() {
        assertEquals(messageTimestamp("2026-10-18T10:00:00Z"), messageTimestamp("2026-10-18T13:00:00+03:00"))
        assertNull(messageTimestamp("invalid"))
        assertNull(messageTimestamp(null))
    }
    @Test fun `restored outbox does not hide cached chat history`() {
        val cached = listOf(message("older"), message("sent", 1))
        val local = listOf(message("sent", 1).copy(isRead = true), message("tmp-failed", 2))
        val merged = mergeCachedMessages(cached, local)
        assertEquals(listOf("older", "sent", "tmp-failed"), merged.map { it.id })
        assertEquals(true, merged[1].isRead)
    }

}
