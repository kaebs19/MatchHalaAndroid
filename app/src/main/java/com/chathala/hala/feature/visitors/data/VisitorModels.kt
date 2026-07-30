package com.chathala.hala.feature.visitors.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** جسم تسجيل زيارة ملف: POST /api/mobile/profile-views */
@JsonClass(generateAdapter = true)
data class RecordViewBody(val viewedUserId: String)

/** ردّ قائمة الزوّار: GET /api/mobile/profile-views */
@JsonClass(generateAdapter = true)
data class ProfileViewsResponse(
    val success: Boolean,
    val data: ProfileViewsData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileViewsData(
    val totalViews: Int = 0,
    val views: List<ProfileViewItem> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    /** true للمجاني — الأسماء والصور مخفية ويجب الترقية لكشفها. */
    val isPremiumRequired: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ProfileViewItem(
    val viewer: Visitor? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Visitor(
    // null للمستخدم المجاني (بيانات مخفية)
    @Json(name = "_id") val id: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val country: String? = null,
    val isVerified: Boolean? = null
)
