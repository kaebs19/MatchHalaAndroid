package com.chathala.hala.feature.verification.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerificationStatusResponse(
    val success: Boolean,
    val data: VerificationStatusData? = null
)

@JsonClass(generateAdapter = true)
data class VerificationStatusData(
    val isVerified: Boolean? = null,
    val status: String? = null,   // none | pending | rejected | verified
    val submittedAt: String? = null,
    val reviewedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VerificationSubmitResponse(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
    val data: VerificationSubmitData? = null
)

@JsonClass(generateAdapter = true)
data class VerificationSubmitData(
    val status: String? = null
)
