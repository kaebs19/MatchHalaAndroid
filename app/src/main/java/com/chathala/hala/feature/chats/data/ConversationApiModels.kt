package com.chathala.hala.feature.chats.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConversationsResponse(
    val success: Boolean,
    val data: ConversationsData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationsData(
    val conversations: List<Conversation> = emptyList(),
    val total: Int = 0,
    val totalUnread: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

/** ردّ جلب محادثة واحدة (GET /conversations/:id) — للطرف الآخر عند فتح الشات بسرعة. */
@JsonClass(generateAdapter = true)
data class SingleConversationResponse(
    val success: Boolean,
    val data: SingleConversationData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SingleConversationData(
    val conversation: Conversation? = null
)

@JsonClass(generateAdapter = true)
data class Conversation(
    @Json(name = "_id") val id: String,
    val type: String? = null,
    val status: String? = null,        // pending | accepted | rejected | expired
    val chatMode: String? = null,      // snap | 24h | keep
    val isActive: Boolean? = null,
    val creator: String? = null,
    val participants: List<Participant> = emptyList(),
    val lastMessage: LastMessage? = null,
    val unreadCount: Int = 0,
    val initialMessage: InitialMessage? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Participant(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val isPremium: Boolean? = null,
    val isOnline: Boolean? = null,
    val lastLogin: String? = null,
    val verification: VerificationFlag? = null,
    /** يرجّعه الخادم = true للحساب الموقوف (بيانات مقنّعة) → نمنع مراسلته. */
    val isSuspendedAccount: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class VerificationFlag(val isVerified: Boolean? = null)

@JsonClass(generateAdapter = true)
data class LastMessage(
    @Json(name = "_id") val id: String? = null,
    val content: String? = null,
    val type: String? = null,
    val sender: String? = null,
    val status: String? = null,
    val isRead: Boolean? = null,
    val isDelivered: Boolean? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class InitialMessage(
    val content: String? = null,
    val createdAt: String? = null
)

// ── pending badge ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class PendingCountResponse(
    val success: Boolean,
    val data: PendingCountData? = null
)

@JsonClass(generateAdapter = true)
data class PendingCountData(
    val total: Int = 0,
    val recent: Int = 0
)
