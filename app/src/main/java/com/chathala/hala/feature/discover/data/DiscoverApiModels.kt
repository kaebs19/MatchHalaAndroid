package com.chathala.hala.feature.discover.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscoverCardsResponse(
    val success: Boolean,
    val data: DiscoverCardsData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DiscoverCardsData(
    val cards: List<DiscoverCard> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

@JsonClass(generateAdapter = true)
data class DiscoverCard(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val photos: List<String> = emptyList(),
    val birthDate: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val bio: String? = null,
    val isOnline: Boolean? = null,
    val isPremium: Boolean? = null,
    val isVerified: Boolean? = null,
    val lastLogin: String? = null,
    val distance: Double? = null,
    val distanceLabel: String? = null
) {
    /** الصور المعروضة في المعرض — تبدأ بـ profileImage ثم الباقي بدون تكرار. */
    val galleryPhotos: List<String>
        get() {
            val main = profileImage?.takeIf { it.isNotBlank() }
            val extra = photos.filter { it.isNotBlank() && it != main }
            return listOfNotNull(main) + extra
        }
}

// ── conversation request (from Discover) ──────────────────────────

@JsonClass(generateAdapter = true)
data class RequestConversationRequest(
    val targetUserId: String,
    val initialMessage: String? = null,
    val isSuperLike: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RequestConversationResponse(
    val success: Boolean,
    val message: String? = null,
    val data: RequestConversationData? = null,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class RequestConversationData(
    val conversation: DiscoverConversationRef? = null,
    val isExisting: Boolean? = null,
    val isSuperLike: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DiscoverConversationRef(
    @Json(name = "_id") val id: String? = null,
    val status: String? = null
)

// ── Swipe (like / dislike / superlike) — POST /api/swipes ──

@JsonClass(generateAdapter = true)
data class SwipeRequest(
    val userId: String,
    val type: String   // like | dislike | superlike
)

@JsonClass(generateAdapter = true)
data class SwipeResponse(
    val success: Boolean,
    val message: String? = null,
    val data: SwipeResponseData? = null
)

@JsonClass(generateAdapter = true)
data class SwipeResponseData(
    val match: SwipeMatch? = null
)

@JsonClass(generateAdapter = true)
data class SwipeMatch(
    val matchId: String? = null,
    val conversationId: String? = null
)
