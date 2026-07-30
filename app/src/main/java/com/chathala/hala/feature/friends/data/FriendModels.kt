package com.chathala.hala.feature.friends.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** حالة الصداقة بيني وبين مستخدم آخر. */
enum class FriendStatus {
    NONE,             // لا شيء — يمكن إرسال طلب
    FRIENDS,          // أصدقاء
    PENDING_SENT,     // أرسلتُ طلباً بانتظار الرد
    PENDING_RECEIVED; // وصلني طلب منه — يمكن القبول/الرفض

    companion object {
        fun fromApi(s: String?): FriendStatus = when (s) {
            "friends" -> FRIENDS
            "pending_sent" -> PENDING_SENT
            "pending_received" -> PENDING_RECEIVED
            else -> NONE
        }
    }
}

// ── مستخدم مختصر داخل عناصر الأصدقاء/الطلبات ──
@JsonClass(generateAdapter = true)
data class FriendUser(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val isOnline: Boolean? = null,
    val lastLogin: String? = null,
    val isPremium: Boolean? = null,
    val isVerified: Boolean? = null,
    val profileCompletion: Double? = null
)

// ── GET /friends/status/:userId ──
@JsonClass(generateAdapter = true)
data class FriendStatusResponse(
    val success: Boolean,
    val data: FriendStatusData? = null
)

@JsonClass(generateAdapter = true)
data class FriendStatusData(
    val status: String? = null,
    val friendshipId: String? = null,
    val since: String? = null
)

// ── POST /friends/request + accept ──
@JsonClass(generateAdapter = true)
data class FriendActionResponse(
    val success: Boolean,
    val message: String? = null,
    val data: FriendActionData? = null
)

@JsonClass(generateAdapter = true)
data class FriendActionData(
    val status: String? = null,
    val friendshipId: String? = null,
    val conversationId: String? = null
)

@JsonClass(generateAdapter = true)
data class FriendRequestBody(val userId: String)

/** PATCH /privacy/friend-requests — من يستطيع إضافتي (everyone|contacts|nobody). */
@JsonClass(generateAdapter = true)
data class UpdateFriendRequestsPrivacyBody(val friendRequests: String)

// ── GET /friends ──
@JsonClass(generateAdapter = true)
data class FriendsListResponse(
    val success: Boolean,
    val data: FriendsListData? = null
)

@JsonClass(generateAdapter = true)
data class FriendsListData(
    val friends: List<FriendItem> = emptyList(),
    val totalCount: Int = 0,
    val pendingCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class FriendItem(
    val friendshipId: String,
    val since: String? = null,
    val conversationId: String? = null,
    val user: FriendUser
)

// ── GET /friends/requests ──
@JsonClass(generateAdapter = true)
data class FriendRequestsResponse(
    val success: Boolean,
    val data: FriendRequestsData? = null
)

@JsonClass(generateAdapter = true)
data class FriendRequestsData(
    val requests: List<FriendRequestItem> = emptyList(),
    val totalCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class FriendRequestItem(
    val friendshipId: String,
    val createdAt: String? = null,
    val user: FriendUser
)
