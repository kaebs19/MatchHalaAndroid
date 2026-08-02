package com.chathala.hala.feature.blocking.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlockedUsersResponse(
    val success: Boolean,
    val data: BlockedUsersData? = null
)

@JsonClass(generateAdapter = true)
data class BlockedUsersData(
    val blockedUsers: List<BlockedUser> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BlockedUser(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val email: String? = null,
    val profileImage: String? = null,
    val isPremium: Boolean? = null,
    val isActive: Boolean? = null,
    val verification: VerificationFlag? = null
)

@JsonClass(generateAdapter = true)
data class VerificationFlag(val isVerified: Boolean? = null)

@JsonClass(generateAdapter = true)
data class BlockStatusResponse(
    val success: Boolean,
    val data: BlockStatusData? = null
)

/** حالة الحظر بين الطرفين — الخادم يُرجع الاتجاهين. */
@JsonClass(generateAdapter = true)
data class BlockStatusData(
    /** أنا حظرته */
    val isBlocked: Boolean = false,
    /** هو حظرني */
    val blockedByThem: Boolean = false,
    /** لا مراسلة بأي اتجاه */
    val cannotContact: Boolean = false
)
