package com.chathala.hala.feature.user.data

import com.squareup.moshi.JsonClass

/**
 * جسم `PUT /api/mobile/users/location`.
 *
 * المدينة والدولة تُترجَم على الجهاز من الإحداثيات (Geocoder) — الخادم لا يملك
 * مُترجِماً عكسياً، وبدونهما يرى الأدمن أرقاماً فقط في لوحة التحكم.
 */
@JsonClass(generateAdapter = true)
data class UpdateLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val country: String? = null,
    /** دقة القراءة بالأمتار — تميّز موقع GPS الحقيقي من تقدير الشبكة الواسع. */
    val accuracy: Float? = null
)

@JsonClass(generateAdapter = true)
data class UpdateLocationResponse(
    val success: Boolean,
    val message: String? = null
)
