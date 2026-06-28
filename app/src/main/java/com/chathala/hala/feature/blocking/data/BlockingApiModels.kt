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
