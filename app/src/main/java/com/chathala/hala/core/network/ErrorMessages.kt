package com.chathala.hala.core.network

/**
 * يحوّل `error.code` من السيرفر إلى رسالة عربية واضحة للمستخدم.
 * إذا الكود غير معروف، يرجع الرسالة الأصلية كما هي.
 *
 * يُستخدم داخل ViewModels:
 * ```
 * is NetworkResult.Error -> state.update {
 *     it.copy(error = ErrorMessages.friendly(r))
 * }
 * ```
 */
object ErrorMessages {

    fun friendly(result: NetworkResult.Error): String =
        when (result.code) {
            // تسجيل / ملف شخصي
            "BANNED_NAME" -> "الاسم غير مسموح — اختر اسماً آخر"
            "NAME_COOLDOWN" -> "يمكنك تغيير الاسم مرة كل 30 يوم فقط"
            "NAME_BLOCKED" -> "تم منعك من تغيير الاسم من قِبل الإدارة"
            "PHOTO_BLOCKED" -> "تم منعك من تغيير الصورة من قِبل الإدارة"
            "PHOTO_COOLDOWN" -> "انتظر دقيقة قبل تغيير الصورة مجدداً"
            "MAX_PHOTOS" -> "وصلت للحد الأقصى من الصور"
            "PREMIUM_REQUIRED" -> "هذه الميزة متاحة للمشتركين فقط"

            // الحساب
            "ACCOUNT_BANNED" -> "تم حظر حسابك بسبب مخالفات متكررة"
            "ACCOUNT_SUSPENDED" -> "حسابك معلّق مؤقتاً"
            "DEVICE_BANNED" -> "هذا الجهاز محظور من استخدام التطبيق"

            // رموز عامة
            "REFRESH_TOKEN_EXPIRED" -> "انتهت جلستك — سجّل الدخول مجدداً"

            else -> result.message
        }

    /** استخراج رسالة مفيدة من أي NetworkResult.Error. */
    fun of(result: NetworkResult<*>): String = when (result) {
        is NetworkResult.Success -> ""
        is NetworkResult.Error -> friendly(result)
    }
}
