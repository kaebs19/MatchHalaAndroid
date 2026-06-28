package com.chathala.hala.core.util

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

    private val arabicMonthYear by lazy {
        SimpleDateFormat("MMMM yyyy", Locale("ar"))
    }

    /** يحوّل ISO timestamp إلى "مارس 2026". يُرجع null إذا فشل التحليل. */
    fun formatJoinDate(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val trimmed = iso.substringBefore('.').trimEnd('Z')
        return runCatching {
            val date = isoParser.parse(trimmed) ?: return null
            arabicMonthYear.format(date)
        }.getOrNull()
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
