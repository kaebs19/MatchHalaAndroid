package com.chathala.hala.feature.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val deviceFingerprint: String? = null,
    val deviceToken: String? = null,
    val vendorId: String? = null,
    val platform: String = "android"
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceFingerprint: String? = null,
    val deviceToken: String? = null,
    val vendorId: String? = null,
    val platform: String = "android",
    val appVersion: String? = null,
    val deviceModel: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    val email: String,
    val resetToken: String,
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean,
    val message: String?,
    val code: String? = null,
    val data: AuthData? = null
)

@JsonClass(generateAdapter = true)
data class AuthData(
    val user: AuthUser?,
    val token: String?,
    val refreshToken: String?,
    val isNewUser: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class AuthUser(
    val id: String?,
    val name: String?,
    val email: String?,
    val role: String?,
    val profileImage: String?
)

@JsonClass(generateAdapter = true)
data class SimpleResponse(
    val success: Boolean,
    val message: String?,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class ErrorBody(
    val success: Boolean? = null,
    val message: String? = null,
    val code: String? = null,
    @Json(name = "errors") val errors: List<ValidationError>? = null,
    // ✅ حقول الإيقاف — يرسلها /auth/login عند code = ACCOUNT_SUSPENDED (403)
    val token: String? = null,
    val user: AuthUser? = null,
    val data: SuspensionPayload? = null
)

@JsonClass(generateAdapter = true)
data class SuspensionPayload(
    val reason: String? = null,
    val suspendedUntil: String? = null,
    val level: Int? = null
)

@JsonClass(generateAdapter = true)
data class ValidationError(
    val field: String?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequest(
    val idToken: String,
    val platform: String = "android",
    val deviceToken: String? = null,
    val deviceFingerprint: String? = null,
    val vendorId: String? = null,
    val deviceInfo: DeviceInfo? = null
)

@JsonClass(generateAdapter = true)
data class DeviceInfo(
    val platform: String = "android",
    val osVersion: String? = null,
    val appVersion: String? = null,
    val deviceModel: String? = null,
    val language: String? = "ar"
)
