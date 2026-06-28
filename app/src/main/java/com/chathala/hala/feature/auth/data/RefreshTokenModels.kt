package com.chathala.hala.feature.auth.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenResponse(
    val success: Boolean,
    val data: RefreshTokenData? = null,
    val message: String? = null,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenData(
    val token: String? = null,
    val refreshToken: String? = null,
    val user: AuthUser? = null
)
