package com.chathala.hala.feature.chats.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessagesResponse(
    val success: Boolean,
    val data: MessagesData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MessagesData(
    val messages: List<Message> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val messagingRestriction: MessagingRestrictionInfo? = null
)

@JsonClass(generateAdapter = true)
data class MessagingRestrictionInfo(
    val restricted: Boolean = false,
    val reason: String? = null,
    val title: String? = null,
    val message: String? = null,
    val hoursLeft: Int? = null,
    val lockedUntil: String? = null
)

@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "_id") val id: String,
    val conversation: String? = null,
    val sender: MessageSender? = null,
    val content: String? = null,
    val type: String? = null,       // text | image | file | audio | video | system
    val mediaUrl: String? = null,
    val status: String? = null,     // sent | delivered | read
    val isRead: Boolean? = null,
    val isDelivered: Boolean? = null,
    val isDeleted: Boolean? = null,
    val isCensored: Boolean? = null,       // admin censored (نجوم)
    val hasFlaggedContent: Boolean? = null,       // sensitive content auto-detected
    val isExternalPromoBlocked: Boolean? = null,  // blocked due to external account sharing
    val externalPromoCategories: List<String>? = null,
    val replyTo: MessageReply? = null,
    // ── تعديل (بريميوم) ──
    val isEdited: Boolean? = null,
    val editedAt: String? = null,
    val createdAt: String? = null,
    // ── audio ──
    // ملاحظة: iOS يرسل waveform كقيم عشرية 0..1 والمدّة قد تُخزَّن كرقم عشري في الخادم،
    // لذا نستقبلهما كـ Double حتى لا يفشل تحليل Moshi ويسقط الرد كاملاً.
    val audioDuration: Double? = null,    // بالثواني
    val audioWaveform: List<Double>? = null,
    // ── image (disappearing) ──
    val imageSource: String? = null,   // camera | gallery
    val disappearing: DisappearingInfo? = null,
    // ── video (مؤقت دائماً: once | 24h) ──
    val videoDuration: Double? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    // ── reactions ──
    val reactions: List<Reaction>? = null
)

@JsonClass(generateAdapter = true)
data class EditMessageRequest(val content: String)

@JsonClass(generateAdapter = true)
data class EditMessageResponse(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
    val data: EditedMessageData? = null
)

@JsonClass(generateAdapter = true)
data class EditedMessageData(
    val messageId: String? = null,
    val conversationId: String? = null,
    val content: String? = null,
    val isEdited: Boolean? = null,
    val editedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Reaction(
    val user: String? = null,
    val emoji: String,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DisappearingInfo(
    val enabled: Boolean? = null,
    /** للصورة: مهلة العرض. ⚠️ للفيديو: **طوله** لا مهلته. */
    val duration: Int? = null,
    val expiresAt: String? = null,
    /** للفيديو فقط: `once` | `24h`. */
    val mode: String? = null,
    val viewedBy: List<DisappearingView>? = null
)

@JsonClass(generateAdapter = true)
data class DisappearingView(
    val user: String? = null,
    val viewedAt: String? = null,
    val expired: Boolean? = null
)

/** وضع الفيديو المؤقت. */
object VideoMode {
    const val ONCE = "once"
    const val DAY = "24h"
}

fun Message.isVideo(): Boolean = type == "video"

@JsonClass(generateAdapter = true)
data class MessageSender(
    @Json(name = "_id") val id: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val isPremium: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class MessageReply(
    @Json(name = "_id") val id: String? = null,
    val content: String? = null,
    val type: String? = null,
    val sender: MessageReplySender? = null
)

@JsonClass(generateAdapter = true)
data class MessageReplySender(
    val name: String? = null
)

// ── send ──────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    val conversationId: String,
    val content: String,
    val type: String = "text",
    val replyTo: String? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageResponse(
    val success: Boolean,
    val data: SendMessageData? = null,
    val message: String? = null,
    val externalPromoBlocked: ExternalPromoBlockedInfo? = null,
    val messagingLocked: MessagingLockedInfo? = null
)

@JsonClass(generateAdapter = true)
data class MessagingLockedInfo(
    val title: String? = null,
    val message: String? = null,
    val serverMessage: String? = null,
    val hoursLeft: Int? = null,
    val lockedUntil: String? = null,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class ExternalPromoBlockedInfo(
    val title: String? = null,
    val message: String? = null,
    val serverMessage: String? = null,
    val categories: List<String>? = null,
    val violations: Int? = null,
    val threshold: Int? = null,
    val lockCount: Int? = null,
    val durationHours: Int? = null,     // صار 0 دائماً — المدة في durationMinutes
    val durationMinutes: Int? = null,
    val severity: String? = null,  // first | repeated | last_warning | locked | paused | suspended
    val lockApplied: Boolean? = null,
    val suspended: Boolean? = null,
    val detectedPatterns: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AppealBlockRequest(
    val reason: String
)

@JsonClass(generateAdapter = true)
data class AppealBlockResponse(
    val success: Boolean,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageData(
    val message: Message? = null
)

// ── reactions ─────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ReactRequest(
    val emoji: String
)

@JsonClass(generateAdapter = true)
data class ReactResponse(
    val success: Boolean,
    val data: ReactResponseData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ReactResponseData(
    val reactions: List<Reaction> = emptyList()
)

// ── mute ──────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class MuteRequest(
    val muted: Boolean,
    val mutedUntil: String? = null
)

@JsonClass(generateAdapter = true)
data class MuteResponse(
    val success: Boolean,
    val muted: Boolean? = null,
    val mutedUntil: String? = null,
    val message: String? = null
)

// ── forward ───────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ForwardRequest(
    val messageId: String,
    val targetConversationId: String
)

@JsonClass(generateAdapter = true)
data class ForwardResponse(
    val success: Boolean,
    val data: SendMessageData? = null,
    val message: String? = null
)

// ── chat mode ─────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ChatModeRequest(
    val chatMode: String   // snap | 24h | keep
)

@JsonClass(generateAdapter = true)
data class ChatModeResponse(
    val success: Boolean,
    val data: ChatModeData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatModeData(
    val chatMode: String? = null,
    val systemMessage: Message? = null
)

@JsonClass(generateAdapter = true)
data class ChatModeInfoResponse(
    val success: Boolean,
    val data: ChatModeInfoData? = null
)

@JsonClass(generateAdapter = true)
data class ChatModeInfoData(
    val chatMode: String? = null
)

// ── sensitive content reveal ──────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RevealContentResponse(
    val success: Boolean,
    val data: RevealContentData? = null,
    val message: String? = null,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class RevealContentData(
    val content: String? = null,
    val revealedAt: String? = null
)

// ── promo keywords ───────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class PromoKeywordsResponse(
    val success: Boolean,
    val data: PromoKeywordsData? = null
)

@JsonClass(generateAdapter = true)
data class PromoKeywordsData(
    val keywords: List<com.chathala.hala.feature.chats.data.PromoKeyword> = emptyList()
)

// ── view disappearing photo ──────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ViewPhotoResponse(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
    val data: ViewPhotoData? = null
)

@JsonClass(generateAdapter = true)
data class ViewPhotoData(
    val duration: Int? = null,
    val mode: String? = null,
    val expiresAt: String? = null,
    val mediaUrl: String? = null
)
