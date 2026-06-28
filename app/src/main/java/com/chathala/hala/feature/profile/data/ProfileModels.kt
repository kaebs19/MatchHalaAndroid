package com.chathala.hala.feature.profile.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Interest(
    val _id: String?,
    val key: String,
    val nameAr: String,
    val nameEn: String?,
    val emoji: String?,
    val category: String?,
    val isActive: Boolean? = true,
    val order: Int? = 0
)

@JsonClass(generateAdapter = true)
data class InterestsResponse(
    val success: Boolean,
    val data: List<Interest> = emptyList(),
    val count: Int? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val bio: String? = null,
    val interests: List<String>? = null,
    val defaultAvatar: String? = null   // avatar_1 .. avatar_29
)

@JsonClass(generateAdapter = true)
data class UpdateProfileResponse(
    val success: Boolean,
    val message: String?,
    val code: String? = null
)

