package com.chathala.hala.feature.discover.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserSearchResponse(
    val success: Boolean,
    val data: UserSearchData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class UserSearchData(
    val users: List<SearchUser> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class SearchUser(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val bio: String? = null,
    val isOnline: Boolean? = null,
    val isVerified: Boolean? = null,
    val isPremium: Boolean? = null,
    val distanceLabel: String? = null
)
