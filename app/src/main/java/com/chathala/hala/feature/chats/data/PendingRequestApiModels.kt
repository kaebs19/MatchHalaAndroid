package com.chathala.hala.feature.chats.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PendingRequestsResponse(
    val success: Boolean,
    val data: PendingRequestsData? = null
)

@JsonClass(generateAdapter = true)
data class PendingRequestsData(
    val conversations: List<PendingRequest> = emptyList(),
    val total: Int = 0,
    val recentCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class PendingRequest(
    @Json(name = "_id") val id: String,
    val status: String? = null,
    val chatMode: String? = null,
    val isSuperLike: Boolean? = null,
    val creator: PendingRequestCreator? = null,
    val initialMessage: InitialMessage? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PendingRequestCreator(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val isPremium: Boolean? = null,
    val isVerified: Boolean? = null
)

// ── accept-with-message ───────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AcceptWithMessageRequest(
    val greeting: String? = null
)

@JsonClass(generateAdapter = true)
data class AcceptWithMessageResponse(
    val success: Boolean,
    val data: AcceptWithMessageData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AcceptWithMessageData(
    val conversationId: String? = null,
    val welcomeMessage: Message? = null
)
