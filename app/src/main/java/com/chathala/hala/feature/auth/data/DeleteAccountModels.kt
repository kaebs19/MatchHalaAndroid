package com.chathala.hala.feature.auth.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeleteAccountRequest(
    val password: String? = null
)
