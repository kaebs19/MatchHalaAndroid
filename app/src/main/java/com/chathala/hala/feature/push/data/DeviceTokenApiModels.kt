package com.chathala.hala.feature.push.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceTokenRequest(
    val deviceToken: String,
    val platform: String = "android",
    val osVersion: String? = null,
    val appVersion: String? = null
)
