package com.chathala.hala.feature.reporting.data

import com.squareup.moshi.JsonClass

/**
 * أسباب البلاغ المدعومة من الـ backend.
 * values مطابقة لما يتحقّق منه validReasons في `/api/mobile/reports`.
 */
enum class ReportReason(val apiValue: String, val displayAr: String) {
    SPAM("spam", "محتوى إزعاجي / سبام"),
    INAPPROPRIATE("inappropriate", "محتوى غير لائق"),
    HARASSMENT("harassment", "مضايقة أو إساءة"),
    FAKE_PROFILE("fake_profile", "حساب مزيّف"),
    OTHER("other", "سبب آخر")
}

@JsonClass(generateAdapter = true)
data class CreateReportRequest(
    val reportedUser: String,
    val reason: String,
    val description: String? = null
)
