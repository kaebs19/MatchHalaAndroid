package com.chathala.hala.core.util

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import android.util.Patterns
import com.chathala.hala.core.config.AppConfig
import com.chathala.hala.core.config.AppConfig.MAX_NAME_LENGTH
import com.chathala.hala.core.config.AppConfig.MIN_NAME_LENGTH
import com.chathala.hala.core.config.AppConfig.MIN_PASSWORD_LENGTH
import java.util.Calendar
import java.util.Date

object Validators {

    // يطابق regex السيرفر: حروف عربية (U+0600 إلى U+06FF) + لاتيني + مسافة
    private val NAME_REGEX = Regex("""^[\u0600-\u06FFa-zA-Z\s]+$""")

    fun email(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> S.get(R.string.valid_email_required)
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> S.get(R.string.valid_email_invalid)
            else -> null
        }
    }

    /**
     * متطلبات كلمة المرور (مطابقة للسيرفر):
     *  - 6 أحرف على الأقل
     *  - حرف إنجليزي صغير + حرف كبير + رقم
     */
    fun password(value: String): String? = when {
        value.isEmpty() -> S.get(R.string.valid_password_required)
        value.length < MIN_PASSWORD_LENGTH -> S.get(R.string.valid_password_min_chars, MIN_PASSWORD_LENGTH)
        !value.any { it.isLowerCase() } -> S.get(R.string.valid_password_lowercase)
        !value.any { it.isUpperCase() } -> S.get(R.string.valid_password_uppercase)
        !value.any { it.isDigit() } -> S.get(R.string.valid_password_digit)
        else -> null
    }

    fun passwordConfirm(password: String, confirm: String): String? =
        if (password != confirm) S.get(R.string.valid_password_mismatch) else null

    fun name(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.length < MIN_NAME_LENGTH -> S.get(R.string.valid_name_min_chars, MIN_NAME_LENGTH)
            trimmed.length > MAX_NAME_LENGTH -> S.get(R.string.valid_name_max_chars, MAX_NAME_LENGTH)
            !NAME_REGEX.matches(trimmed) -> S.get(R.string.valid_name_letters_only)
            else -> null
        }
    }

    fun birthDate(date: Date?): String? {
        if (date == null) return S.get(R.string.valid_birthdate_required)
        val age = ageFromDate(date)
        return when {
            age < AppConfig.MIN_AGE -> S.get(R.string.valid_min_age, AppConfig.MIN_AGE)
            age > AppConfig.MAX_AGE -> S.get(R.string.valid_birthdate_invalid)
            else -> null
        }
    }

    fun ageFromDate(date: Date): Int {
        val today = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { time = date }
        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age
    }
}
