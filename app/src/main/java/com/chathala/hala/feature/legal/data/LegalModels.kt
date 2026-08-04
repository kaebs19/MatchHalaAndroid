package com.chathala.hala.feature.legal.data

import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
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

enum class LegalDocType(private val titleRes: Int) {
    PRIVACY(R.string.legal_privacy_policy),
    TERMS(R.string.legal_terms_of_use);

    /** يُقرأ وقت العرض — ثوابت الـ enum تُهيّأ مرة واحدة فقط. */
    val title: String get() = S.get(titleRes)
}
