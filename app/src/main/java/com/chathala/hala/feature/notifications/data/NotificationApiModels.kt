package com.chathala.hala.feature.notifications.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationsResponse(
    val success: Boolean,
    val data: NotificationsData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationsData(
    val notifications: List<NotificationItem> = emptyList(),
    val total: Int = 0,
    val unreadCount: Int = 0,
    val allCount: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val filter: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
    @Json(name = "_id") val id: String,
    val title: String? = null,
    val body: String? = null,
    val type: String? = null,
    val category: String? = null,
    val image: String? = null,
    val data: Map<String, Any?>? = null,
    val sender: NotificationSender? = null,
    val readBy: List<ReadByEntry>? = null,
    val grouped: Boolean? = null,
    val groupCount: Int? = null,
    val recipients: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationSender(
    @Json(name = "_id") val id: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val isPremium: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ReadByEntry(
    val user: String? = null,
    val readAt: String? = null
)
