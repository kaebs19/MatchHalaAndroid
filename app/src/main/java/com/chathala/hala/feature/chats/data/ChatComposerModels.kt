package com.chathala.hala.feature.chats.data

import com.chathala.hala.core.network.NetworkResult

data class ChatDraft(val text: String = "", val reply: MessageReply? = null) {
    val isEmpty: Boolean get() = text.isEmpty() && reply == null
}

enum class OutgoingStatus { PREPARING, SENDING, FAILED, REJECTED, SENT }

data class OutgoingMessage(
    val localId: String,
    val message: Message,
    val status: OutgoingStatus = OutgoingStatus.PREPARING,
    val attachmentPath: String? = null,
    val failure: String? = null,
    val retryAllowed: Boolean = true,
    val deliveryUncertain: Boolean = false,
    val errorCode: String? = null,
    val promo: ExternalPromoBlockedInfo? = null,
    val locked: MessagingLockedInfo? = null
) {
    val busy: Boolean get() = status == OutgoingStatus.PREPARING || status == OutgoingStatus.SENDING
    fun displayMessage(): Message = if (status == OutgoingStatus.SENT) message else message.copy(status = status.name.lowercase())
}

data class OutgoingSendResult(
    val result: NetworkResult<Message>,
    val promo: ExternalPromoBlockedInfo? = null,
    val locked: MessagingLockedInfo? = null
)

data class OutgoingLocalFailure(val sessionId: String, val conversationId: String, val entry: OutgoingMessage)

private val permanentSendRejections = setOf(
    "USER_BLOCKED", "ACCOUNT_BANNED", "ACCOUNT_SUSPENDED", "DEVICE_BANNED",
    "CONVERSATION_PENDING", "CONVERSATION_CANCELLED", "CONVERSATION_INACTIVE",
    "RECIPIENT_SUSPENDED", "EXTERNAL_PROMO_BLOCKED", "MESSAGING_RESTRICTED",
    "MESSAGING_LOCKED", "PREMIUM_REQUIRED", "REFRESH_TOKEN_EXPIRED",
    // رفض نهائي للفيديو — لا طابور ولا إعادة تلقائية
    "VIDEO_LIMIT_REACHED", "VIDEO_TOO_LARGE", "VIDEO_TOO_LONG", "VIDEO_INVALID"
)

/** Server restrictions take precedence; other temporary HTTP failures permit a manual retry. */
internal fun NetworkResult.Error.permitsMessageRetry(): Boolean =
    code !in permanentSendRejections &&
        ((httpCode == null && code == null) || httpCode == 408 || httpCode == 429 || (httpCode ?: 0) >= 500)

internal fun NetworkResult.Error.hasUncertainDelivery(): Boolean =
    httpCode == null || httpCode == 408 || (httpCode ?: 0) >= 500
