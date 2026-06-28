package com.chathala.hala.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ZodiacSign(val nameAr: String, val emoji: String)

/**
 * يحسب البرج من تاريخ الميلاد (ISO أو yyyy-MM-dd).
 */
object Zodiac {
    fun fromBirthDate(birthDate: String?): ZodiacSign? {
        if (birthDate.isNullOrBlank()) return null
        return runCatching {
            val date = parseDate(birthDate) ?: return null
            compute(date.monthValue, date.dayOfMonth)
        }.getOrNull()
    }

    private fun parseDate(raw: String): LocalDate? {
        return runCatching {
            if (raw.length >= 10) LocalDate.parse(raw.substring(0, 10))
            else LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull()
    }

    private fun compute(month: Int, day: Int): ZodiacSign = when {
        (month == 3 && day >= 21) || (month == 4 && day <= 19) -> ZodiacSign("الحمل", "♈️")
        (month == 4 && day >= 20) || (month == 5 && day <= 20) -> ZodiacSign("الثور", "♉️")
        (month == 5 && day >= 21) || (month == 6 && day <= 20) -> ZodiacSign("الجوزاء", "♊️")
        (month == 6 && day >= 21) || (month == 7 && day <= 22) -> ZodiacSign("السرطان", "♋️")
        (month == 7 && day >= 23) || (month == 8 && day <= 22) -> ZodiacSign("الأسد", "♌️")
        (month == 8 && day >= 23) || (month == 9 && day <= 22) -> ZodiacSign("العذراء", "♍️")
        (month == 9 && day >= 23) || (month == 10 && day <= 22) -> ZodiacSign("الميزان", "♎️")
        (month == 10 && day >= 23) || (month == 11 && day <= 21) -> ZodiacSign("العقرب", "♏️")
        (month == 11 && day >= 22) || (month == 12 && day <= 21) -> ZodiacSign("القوس", "♐️")
        (month == 12 && day >= 22) || (month == 1 && day <= 19) -> ZodiacSign("الجدي", "♑️")
        (month == 1 && day >= 20) || (month == 2 && day <= 18) -> ZodiacSign("الدلو", "♒️")
        else -> ZodiacSign("الحوت", "♓️")
    }

    /** يُرجع تاريخ الميلاد بصيغة "4 Apr" مطابق iOS. */
    fun birthdayLabel(birthDate: String?): String? {
        if (birthDate.isNullOrBlank()) return null
        return runCatching {
            val date = parseDate(birthDate) ?: return null
            val month = when (date.monthValue) {
                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                else -> ""
            }
            "${date.dayOfMonth} $month"
        }.getOrNull()
    }
}
