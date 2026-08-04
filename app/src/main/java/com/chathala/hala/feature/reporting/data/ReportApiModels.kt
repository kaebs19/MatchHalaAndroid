package com.chathala.hala.feature.reporting.data

import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.squareup.moshi.JsonClass

/**
 * أسباب البلاغ المدعومة من الـ backend.
 * values مطابقة لما يتحقّق منه validReasons في `/api/mobile/reports`.
 */
enum class ReportReason(val apiValue: String, private val labelRes: Int) {
    SPAM("spam", R.string.report_reason_spam),
    INAPPROPRIATE("inappropriate", R.string.report_reason_inappropriate),
    HARASSMENT("harassment", R.string.report_reason_harassment),
    FAKE_PROFILE("fake_profile", R.string.report_reason_fake),
    OTHER("other", R.string.report_reason_other);

    /**
     * النص المعروض باللغة الحالية.
     *
     * نخزّن معرّف المورد لا النص: ثوابت الـ enum تُهيّأ مرة واحدة عند تحميل الصنف،
     * فالنص المُترجَم كان سيتجمّد على لغة الإقلاع.
     */
    val label: String get() = S.get(labelRes)
}

@JsonClass(generateAdapter = true)
data class CreateReportRequest(
    val reportedUser: String,
    val reason: String,
    val description: String? = null
)
