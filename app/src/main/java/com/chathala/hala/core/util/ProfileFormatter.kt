package com.chathala.hala.core.util

import com.chathala.hala.R
import com.chathala.hala.core.i18n.LocaleManager
import com.chathala.hala.core.i18n.S

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * محوّلات لعرض بيانات الملف الشخصي.
 */
object ProfileFormatter {

    private val isoParser by lazy {
        // يقبل 2026-03-15T00:00:00.000Z أو 2026-03-15T00:00:00Z
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = true
        }
    }

    // لا نستخدم `by lazy`: كان يجمّد اللغة على أول استدعاء فتبقى أسماء الأشهر عربية
    // بعد التبديل للإنجليزية. البناء عند الطلب رخيص عند مواضع الاستدعاء هذه.
    private val monthYear: SimpleDateFormat
        get() = SimpleDateFormat("MMMM yyyy", LocaleManager.locale)

    /** يحوّل ISO timestamp إلى "مارس 2026". يُرجع null إذا فشل التحليل. */
    fun formatJoinDate(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val trimmed = iso.substringBefore('.').trimEnd('Z')
        return runCatching {
            val date = isoParser.parse(trimmed) ?: return null
            monthYear.format(date)
        }.getOrNull()
    }

    /** أقصى قِدَم يُعتبر معه المستخدم «نشطاً مؤخراً»: أسبوع (نفس نافذة الخادم `7d`). */
    const val RECENT_ACTIVITY_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

    /**
     * وسم آخر ظهور: «قبل 5 دقائق» / «أمس»… يُرجع null إذا لم يكن التاريخ صالحاً
     * أو تجاوز [RECENT_ACTIVITY_WINDOW_MS] — «قبل 40 يوماً» ليس نشاطاً يستحق العرض.
     */
    fun lastActiveLabel(iso: String?): String? {
        val elapsed = millisSince(iso) ?: return null
        if (elapsed > RECENT_ACTIVITY_WINDOW_MS) return null
        val minutes = elapsed / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> S.get(R.string.time_moments_ago)
            minutes < 60 -> S.plural(R.plurals.time_minutes_ago, minutes.toInt())
            hours < 24 -> S.plural(R.plurals.time_hours_ago, hours.toInt())
            else -> S.plural(R.plurals.time_days_ago, days.toInt())
        }
    }

    /**
     * كم مضى على تاريخ ISO بالمللي ثانية، أو null إن تعذّر التحليل.
     * تواريخ المستقبل (انحراف ساعة الجهاز) تُعامَل كـ«الآن» بدل قيمة سالبة.
     */
    fun millisSince(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val trimmed = iso.substringBefore('.').trimEnd('Z')
        val date = runCatching { isoParser.parse(trimmed) }.getOrNull() ?: return null
        return (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
    }

    /** يحسب العمر من تاريخ الميلاد ISO. */
    fun computeAge(birthDateIso: String?): Int? {
        if (birthDateIso.isNullOrBlank()) return null
        val trimmed = birthDateIso.substringBefore('.').trimEnd('Z')
        val birth = runCatching { isoParser.parse(trimmed) }.getOrNull() ?: return null
        return ageFromDate(birth)
    }

    private fun ageFromDate(birth: Date): Int {
        val today = Calendar.getInstance()
        val b = Calendar.getInstance().apply { time = birth }
        var age = today.get(Calendar.YEAR) - b.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < b.get(Calendar.DAY_OF_YEAR)) age--
        return age.coerceAtLeast(0)
    }

    /** مثال: "HALA" + آخر 6 أحرف من الـ _id بحروف كبيرة = "HALAGMWTD6". */
    fun formatUserId(id: String): String {
        val suffix = id.takeLast(6).uppercase()
        return "HALA$suffix"
    }

    /**
     * يحسب نسبة اكتمال الملف الشخصي (0..100).
     * الأوزان:
     *  - صورة شخصية       20%
     *  - نبذة             20%
     *  - جنس              15%
     *  - دولة             15%
     *  - تاريخ ميلاد       15%
     *  - 3 اهتمامات أو أكثر 15%
     */
    fun computeCompletionPercent(
        hasProfileImage: Boolean,
        bio: String?,
        gender: String?,
        country: String?,
        birthDate: String?,
        interestsCount: Int
    ): Int {
        var score = 0
        if (hasProfileImage) score += 20
        if (!bio.isNullOrBlank()) score += 20
        if (!gender.isNullOrBlank()) score += 15
        if (!country.isNullOrBlank()) score += 15
        if (!birthDate.isNullOrBlank()) score += 15
        if (interestsCount >= 3) score += 15
        return score.coerceIn(0, 100)
    }
}
