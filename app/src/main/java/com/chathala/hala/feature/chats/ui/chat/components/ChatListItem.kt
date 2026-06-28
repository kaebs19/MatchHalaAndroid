package com.chathala.hala.feature.chats.ui.chat.components

import com.chathala.hala.feature.chats.data.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * عنصر واحد في القائمة — إما رسالة، فاصل تاريخ، أو فاصل "غير مقروءة".
 */
sealed class ChatListItem {
    abstract val key: String

    data class MessageItem(
        val message: Message,
        // أول رسالة في مجموعة متتالية من نفس المرسل (مسافة أكبر فوقها)
        val isFirstInGroup: Boolean = true,
        // آخر رسالة في المجموعة (تُعرض عندها الطوابع الزمنية/الحالة)
        val isLastInGroup: Boolean = true
    ) : ChatListItem() {
        override val key: String get() = message.id
    }

    data class DateHeader(val label: String) : ChatListItem() {
        override val key: String get() = "date-$label"
    }

    data object UnreadDivider : ChatListItem() {
        override val key: String get() = "unread-divider"
    }
}

/**
 * يبني قائمة العرض من الرسائل: يدمج date headers قبل أول رسالة لكل يوم،
 * ويُدخل خط "غير المقروءة" قبل أول رسالة غير مقروءة.
 */
fun buildChatList(
    messages: List<Message>,
    firstUnreadId: String?
): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()
    val out = mutableListOf<ChatListItem>()
    var lastDay: String? = null
    // نافذة التجميع: رسائل نفس المرسل خلال دقيقتين تُعتبر مجموعة واحدة
    val groupWindowMs = 2 * 60 * 1000L
    for ((i, msg) in messages.withIndex()) {
        val day = dayKey(msg.createdAt)
        val newDay = day != null && day != lastDay
        if (newDay) {
            out += ChatListItem.DateHeader(labelFor(day))
            lastDay = day
        }
        var unreadHere = false
        if (firstUnreadId != null && msg.id == firstUnreadId) {
            out += ChatListItem.UnreadDivider
            unreadHere = true
        }

        // تجميع: أول في المجموعة إذا اختلف المرسل/اليوم/وُجد فاصل، أو فجوة زمنية كبيرة
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        val isFirst = newDay || unreadHere || prev == null ||
            prev.sender?.id != msg.sender?.id ||
            timeGap(prev.createdAt, msg.createdAt) > groupWindowMs
        val nextNewDay = next != null && dayKey(next.createdAt) != day
        val nextUnread = firstUnreadId != null && next?.id == firstUnreadId
        val isLast = next == null || nextNewDay || nextUnread ||
            next.sender?.id != msg.sender?.id ||
            timeGap(msg.createdAt, next.createdAt) > groupWindowMs

        out += ChatListItem.MessageItem(msg, isFirstInGroup = isFirst, isLastInGroup = isLast)
    }
    return out
}

/** الفارق الزمني بالمللي ثانية بين رسالتين (أو Long.MAX عند الفشل). */
private fun timeGap(a: String?, b: String?): Long {
    val da = a?.let { parseIso(it) } ?: return Long.MAX_VALUE
    val db = b?.let { parseIso(it) } ?: return Long.MAX_VALUE
    return kotlin.math.abs(db.time - da.time)
}

private fun dayKey(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val d = parseIso(iso) ?: return null
    val cal = Calendar.getInstance().apply { time = d }
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

private fun labelFor(day: String): String {
    val parts = day.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return day
    val cal = Calendar.getInstance().apply {
        set(parts[0], parts[1] - 1, parts[2], 0, 0, 0); set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val diffDays = ((today.timeInMillis - cal.timeInMillis) / (24L * 60 * 60 * 1000)).toInt()
    return when (diffDays) {
        0 -> "اليوم"
        1 -> "الأمس"
        in 2..6 -> arabicDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
        else -> {
            val fmt = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
            fmt.format(cal.time)
        }
    }
}

private fun arabicDayOfWeek(d: Int): String = when (d) {
    Calendar.SATURDAY -> "السبت"
    Calendar.SUNDAY -> "الأحد"
    Calendar.MONDAY -> "الإثنين"
    Calendar.TUESDAY -> "الثلاثاء"
    Calendar.WEDNESDAY -> "الأربعاء"
    Calendar.THURSDAY -> "الخميس"
    Calendar.FRIDAY -> "الجمعة"
    else -> ""
}

private fun parseIso(s: String): Date? = try {
    val cleaned = s.substringBefore(".").substringBefore("Z")
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    fmt.parse(cleaned)
} catch (_: Throwable) {
    null
}
