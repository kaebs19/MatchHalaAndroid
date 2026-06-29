package com.chathala.hala.feature.chats.socket

import org.json.JSONObject

/**
 * جميع الأحداث التي نستقبلها من Socket.IO server.
 *
 * نمرّر الـ JSONObject الخام — الـ ViewModels تحوّل إلى domain حسب حاجتها.
 * هذا يحافظ على مرونة السوكت دون إلزام بتحويل موحّد.
 */
sealed class SocketEvent {
    object Authenticated : SocketEvent()

    /** السيرفر يطلب FCM token جديد (missing / failures / stale). */
    data class RequestFcmToken(val reason: String?) : SocketEvent()

    /** رسالة جديدة (في أي محادثة — الـ VM يفلتر). */
    data class NewMessage(val json: JSONObject) : SocketEvent()

    data class UserTyping(val json: JSONObject) : SocketEvent()
    data class MessagesRead(val json: JSONObject) : SocketEvent()
    data class MessageDelivered(val json: JSONObject) : SocketEvent()

    data class ConversationRequest(val json: JSONObject) : SocketEvent()
    data class ConversationAccepted(val json: JSONObject) : SocketEvent()
    data class ConversationRejected(val json: JSONObject) : SocketEvent()
    data class ConversationCancelled(val json: JSONObject) : SocketEvent()
    data class ChatModeChanged(val json: JSONObject) : SocketEvent()

    data class UsersOnline(val json: JSONObject) : SocketEvent()
    data class UserOnline(val json: JSONObject) : SocketEvent()
    data class UserOffline(val json: JSONObject) : SocketEvent()

    data class MessageReaction(val json: JSONObject) : SocketEvent()
    data class MessageDeleted(val json: JSONObject) : SocketEvent()
    data class PhotoViewed(val json: JSONObject) : SocketEvent()

    /** رُفِع تقييد المراسلة عن المستخدم (فوري) */
    data class RestrictionLifted(val json: JSONObject) : SocketEvent()

    /** ردّ جديد من المشرف على استئناف/مراجعة */
    data class AppealReply(val json: JSONObject) : SocketEvent()
}
