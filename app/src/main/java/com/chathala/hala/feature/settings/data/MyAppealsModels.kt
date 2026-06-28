package com.chathala.hala.feature.settings.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── طلباتي (الاستئنافات/المراجعات) ─────────────────────────────
// يعيد استخدام نظام /api/appeals نفسه الذي يستخدمه iOS ولوحة الأدمن.

@JsonClass(generateAdapter = true)
data class MyAppealsResponse(
    val success: Boolean = false,
    val data: List<AppealItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AppealDetailResponse(
    val success: Boolean = false,
    val data: AppealItem? = null
)

@JsonClass(generateAdapter = true)
data class AppealItem(
    @Json(name = "_id") val id: String = "",
    val reason: String? = null,
    val status: String? = null,         // pending | forwarded | under_review | approved | rejected
    val actionType: String? = null,     // suspension | ban | device_ban | restriction
    val adminNote: String? = null,
    val messages: List<AppealMessage> = emptyList(),
    val unreadForUser: Int? = null,
    val createdAt: String? = null,
    val resolvedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AppealMessage(
    @Json(name = "_id") val id: String? = null,
    val sender: String? = null,         // user | admin
    val content: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AppealReplyRequest(
    val content: String
)

@JsonClass(generateAdapter = true)
data class AppealReplyResponse(
    val success: Boolean = false,
    val data: AppealItem? = null,
    val message: String? = null
)
