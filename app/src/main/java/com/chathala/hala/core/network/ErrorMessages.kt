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

            // المحادثات — رفض نهائي من الخادم (لا إعادة إرسال)
            "USER_BLOCKED" -> "لا يمكن التفاعل مع هذا المستخدم"
            "PENDING_REQUEST_LIMIT" ->
                "أرسلت طلبك بالفعل — انتظر رد الطرف الآخر قبل إرسال رسالة أخرى"
            "CONVERSATION_PENDING" -> "لا يمكنك الإرسال حتى تُقبل المحادثة"
            "CONVERSATION_CANCELLED" -> "انتهت هذه المحادثة — أرسل طلباً جديداً للاستئناف"
            "CONVERSATION_INACTIVE" -> "المحادثة غير نشطة"
            "RECIPIENT_SUSPENDED" -> "لا يمكن إرسال رسالة — المستخدم موقوف"

            // تعديل الرسالة (بريميوم)
            "EDIT_WINDOW_EXPIRED" -> "انتهت مهلة تعديل هذه الرسالة (١٥ دقيقة)"
            "NOT_TEXT_MESSAGE" -> "يمكن تعديل الرسائل النصّية فقط"
            "NOT_SENDER" -> "لا يمكنك تعديل رسالة شخص آخر"
            "EXTERNAL_PROMO_BLOCKED" -> "تم حجب التعديل — لا يمكن مشاركة حسابات خارجية"

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
