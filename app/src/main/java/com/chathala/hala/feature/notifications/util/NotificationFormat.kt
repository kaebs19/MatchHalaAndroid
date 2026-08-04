package com.chathala.hala.feature.notifications.util

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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

    /**
     * "قبل 5 دقائق" / "أمس" / "3 days ago"… حسب اللغة الحالية.
     *
     * التفريعات على 1/2/<11 اختفت: موارد `plurals` تختار الصيغة الصحيحة لكل لغة
     * (العربية ستّ صيغ، الإنجليزية اثنتان) بدل تكرار قواعد العربية في الكود.
     */
    fun timeAgo(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val trimmed = iso.substringBefore('.').trimEnd('Z')
        val date = runCatching { isoParser.parse(trimmed) }.getOrNull() ?: return ""
        val diffMs = System.currentTimeMillis() - date.time
        if (diffMs < 0) return S.get(R.string.time_now)

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> S.get(R.string.time_moments_ago)
            minutes < 60 -> S.plural(R.plurals.time_minutes_ago, minutes.toInt())
            hours < 24 -> S.plural(R.plurals.time_hours_ago, hours.toInt())
            else -> S.plural(R.plurals.time_days_ago, days.toInt())
        }
    }

    /** هل قرأ المستخدم الحالي هذا الإشعار؟ */
    fun isReadByCurrentUser(item: NotificationItem, userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        val list = item.readBy ?: return false
        return list.any { it.user == userId }
    }
}
