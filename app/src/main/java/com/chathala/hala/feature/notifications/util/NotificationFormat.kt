package com.chathala.hala.feature.notifications.util

import com.chathala.hala.feature.notifications.data.NotificationItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * أدوات عرض للإشعارات — وقت نسبي + فحص "مقروء/غير مقروء" من قبل المستخدم.
 */
object NotificationFormat {

    private val isoParser by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = true
        }
    }

    /** "قبل 5 دقائق" / "أمس" / "قبل 3 أيام"… */
    fun timeAgoArabic(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val trimmed = iso.substringBefore('.').trimEnd('Z')
        val date = runCatching { isoParser.parse(trimmed) }.getOrNull() ?: return ""
        val diffMs = System.currentTimeMillis() - date.time
        if (diffMs < 0) return "الآن"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "قبل لحظات"
            minutes == 1L -> "قبل دقيقة"
            minutes == 2L -> "قبل دقيقتين"
            minutes < 11 -> "قبل $minutes دقائق"
            minutes < 60 -> "قبل $minutes دقيقة"
            hours == 1L -> "قبل ساعة"
            hours == 2L -> "قبل ساعتين"
            hours < 11 -> "قبل $hours ساعات"
            hours < 24 -> "قبل $hours ساعة"
            days == 1L -> "أمس"
            days == 2L -> "قبل يومين"
            days < 11 -> "قبل $days أيام"
            else -> "قبل $days يوم"
        }
    }

    /** هل قرأ المستخدم الحالي هذا الإشعار؟ */
    fun isReadByCurrentUser(item: NotificationItem, userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        val list = item.readBy ?: return false
        return list.any { it.user == userId }
    }
}
