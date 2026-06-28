package com.chathala.hala.core.util

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
            trimmed.isEmpty() -> "البريد الإلكتروني مطلوب"
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "البريد الإلكتروني غير صحيح"
            else -> null
        }
    }

    /**
     * متطلبات كلمة المرور (مطابقة للسيرفر):
     *  - 6 أحرف على الأقل
     *  - حرف إنجليزي صغير + حرف كبير + رقم
     */
    fun password(value: String): String? = when {
        value.isEmpty() -> "كلمة المرور مطلوبة"
        value.length < MIN_PASSWORD_LENGTH -> "كلمة المرور يجب أن تكون $MIN_PASSWORD_LENGTH أحرف على الأقل"
        !value.any { it.isLowerCase() } -> "يجب أن تحتوي على حرف إنجليزي صغير"
        !value.any { it.isUpperCase() } -> "يجب أن تحتوي على حرف إنجليزي كبير"
        !value.any { it.isDigit() } -> "يجب أن تحتوي على رقم"
        else -> null
    }

    fun passwordConfirm(password: String, confirm: String): String? =
        if (password != confirm) "كلمتا المرور غير متطابقتين" else null

    fun name(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.length < MIN_NAME_LENGTH -> "الاسم يجب أن يكون $MIN_NAME_LENGTH حرفين على الأقل"
            trimmed.length > MAX_NAME_LENGTH -> "الاسم يجب أن لا يتجاوز $MAX_NAME_LENGTH حرفاً"
            !NAME_REGEX.matches(trimmed) -> "الاسم يجب أن يحتوي على حروف فقط (عربية أو إنجليزية)"
            else -> null
        }
    }

    fun birthDate(date: Date?): String? {
        if (date == null) return "تاريخ الميلاد مطلوب"
        val age = ageFromDate(date)
        return when {
            age < AppConfig.MIN_AGE -> "يجب أن يكون عمرك ${AppConfig.MIN_AGE} سنة على الأقل"
            age > AppConfig.MAX_AGE -> "تاريخ الميلاد غير صحيح"
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
