package com.chathala.hala.feature.settings.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── حالة الحساب (account-standing) ──────────────────────────────

@JsonClass(generateAdapter = true)
data class AccountStandingResponse(
    val success: Boolean = false,
    val data: AccountStandingData? = null
)

@JsonClass(generateAdapter = true)
data class AccountStandingData(
    val standing: String = "good",   // good | warning | restricted | suspended
    val violations: Int = 0,
    val softThreshold: Int = 5,
    val hardThreshold: Int = 10,
    val lockCount: Int = 0,
    val reviewable: Boolean = true,
    val nonReviewableNote: String? = null,
    val restriction: StandingRestriction? = null,
    val suspension: StandingSuspension? = null,
    val decay: StandingDecay? = null,
    val escalation: StandingEscalation? = null
)

@JsonClass(generateAdapter = true)
data class StandingRestriction(
    val active: Boolean = false,
    val hoursLeft: Int = 0,
    val until: String? = null,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class StandingSuspension(
    val active: Boolean = false,
    val until: String? = null
)

@JsonClass(generateAdapter = true)
data class StandingDecay(
    val lockDecayDays: Int = 90,
    val daysUntilLockReset: Int? = null
)

@JsonClass(generateAdapter = true)
data class StandingEscalation(
    val currentLockCount: Int = 0,
    val nextStep: Int = 1,
    val ladder: List<EscalationStep> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EscalationStep(
    val step: Int = 0,
    val label: String = "",
    val hours: Int = 0
)

// ── سجل المخالفات (violations-history) ──────────────────────────

@JsonClass(generateAdapter = true)
data class ViolationsHistoryResponse(
    val success: Boolean = false,
    val data: ViolationsHistoryData? = null
)

@JsonClass(generateAdapter = true)
data class ViolationsHistoryData(
    val violations: List<ViolationEntry> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ViolationEntry(
    @Json(name = "_id") val id: String = "",
    val type: String? = null,
    val reason: String? = null,
    val action: String? = null,
    val escalationLevel: Int? = null,
    val source: String? = null,
    val maskedEvidence: String? = null,
    val createdAt: String? = null
)
