package com.chathala.hala.feature.chats.data

import java.time.Instant

/** Preserve messages added/edited/deleted while the HTTP snapshot was in flight. */
internal fun mergeFetchedMessages(fetched: List<Message>, before: List<Message>, current: List<Message>): List<Message> {
    val baseline = before.associateBy { it.id }
    val changed = current.filter { baseline[it.id] != it || it.id.startsWith("tmp-") }
    return (fetched + changed).associateBy { it.id }.values
        .sortedBy { messageTimestamp(it.createdAt) ?: 0L }
}

internal fun mergeCachedMessages(cached: List<Message>, current: List<Message>): List<Message> =
    (cached + current).associateBy { it.id }.values.sortedBy { messageTimestamp(it.createdAt) ?: 0L }

internal fun messageTimestamp(value: String?): Long? =
    value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
