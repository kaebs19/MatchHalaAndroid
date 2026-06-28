package com.chathala.hala.feature.legal.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LegalResponse(
    val success: Boolean,
    val data: LegalData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LegalData(
    val content: String?,
    val lastUpdated: String? = null
)

enum class LegalDocType(val title: String) {
    PRIVACY("سياسة الخصوصية"),
    TERMS("شروط الاستخدام")
}
