package com.chathala.hala.core.network

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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
            "BANNED_NAME" -> S.get(R.string.err_banned_name)
            "NAME_COOLDOWN" -> S.get(R.string.err_name_cooldown)
            "NAME_BLOCKED" -> S.get(R.string.err_name_blocked)
            "PHOTO_BLOCKED" -> S.get(R.string.err_photo_blocked)
            "PHOTO_COOLDOWN" -> S.get(R.string.err_photo_cooldown)
            "MAX_PHOTOS" -> S.get(R.string.err_max_photos)
            "PREMIUM_REQUIRED" -> S.get(R.string.err_premium_required)

            // الحساب
            "ACCOUNT_BANNED" -> S.get(R.string.err_account_banned)
            "ACCOUNT_SUSPENDED" -> S.get(R.string.err_account_suspended)
            "DEVICE_BANNED" -> S.get(R.string.err_device_banned)

            // المحادثات — رفض نهائي من الخادم (لا إعادة إرسال)
            "USER_BLOCKED" -> S.get(R.string.err_cannot_interact)
            "PENDING_REQUEST_LIMIT" ->
                S.get(R.string.err_request_already_sent)
            "CONVERSATION_PENDING" -> S.get(R.string.err_chat_not_accepted)
            "CONVERSATION_CANCELLED" -> S.get(R.string.err_chat_ended)
            "CONVERSATION_INACTIVE" -> S.get(R.string.err_chat_inactive)
            "RECIPIENT_SUSPENDED" -> S.get(R.string.err_user_suspended_send)

            // تعديل الرسالة (بريميوم)
            "EDIT_WINDOW_EXPIRED" -> S.get(R.string.err_edit_window_expired)
            "NOT_TEXT_MESSAGE" -> S.get(R.string.err_edit_text_only)
            "NOT_SENDER" -> S.get(R.string.err_edit_not_owner)
            "EXTERNAL_PROMO_BLOCKED" -> S.get(R.string.err_edit_blocked_promo)

            // رموز عامة
            "REFRESH_TOKEN_EXPIRED" -> S.get(R.string.err_session_expired)

            else -> result.message
        }

    /** استخراج رسالة مفيدة من أي NetworkResult.Error. */
    fun of(result: NetworkResult<*>): String = when (result) {
        is NetworkResult.Success -> ""
        is NetworkResult.Error -> friendly(result)
    }
}
