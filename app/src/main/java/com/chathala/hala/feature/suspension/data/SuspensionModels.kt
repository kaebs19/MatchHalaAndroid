package com.chathala.hala.feature.suspension.data

import com.squareup.moshi.JsonClass

/**
 * نوع الحظر الذي تعرضه شاشة [com.chathala.hala.feature.suspension.ui.SuspendedScreen].
 *  - [ACCOUNT]: الحساب موقوف (مؤقت أو دائم) — لدينا توكن قصير يسمح باستئناف موثّق.
 *  - [DEVICE] : الجهاز محظور — لا توجد جلسة، يُجلب التفصيل من /auth/check-device-ban
 *               ويُقدَّم الاستئناف عبر المسار العام مع بريد للتواصل.
 */
enum class SuspensionMode(val routeArg: String) {
    ACCOUNT("account"),
    DEVICE("device");

    companion object {
        fun fromArg(arg: String?): SuspensionMode =
            entries.firstOrNull { it.routeArg == arg } ?: ACCOUNT
    }
}

/**
 * تفاصيل إيقاف الحساب المستخرجة من رد 403 (ACCOUNT_SUSPENDED).
 *
 * @param token توكن قصير العمر (ساعة) من الرد — يُستخدم لتقديم استئناف موثّق فقط،
 *              ولا يُحفظ في tokenStorage حتى لا يُفهَم كجلسة دخول صالحة.
 */
data class AccountSuspensionInfo(
    val name: String?,
    val email: String?,
    val userId: String?,
    val reason: String?,
    val suspendedUntil: String?,
    val level: Int,
    val isPermanent: Boolean,
    val token: String?
)

// ── /auth/check-device-ban ──────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CheckDeviceBanRequest(
    val deviceFingerprint: String? = null,
    val deviceToken: String? = null,
    val vendorId: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckDeviceBanResponse(
    val success: Boolean,
    val message: String? = null,
    val data: DeviceBanData? = null
)

@JsonClass(generateAdapter = true)
data class DeviceBanData(
    val banned: Boolean = false,
    val bannedDeviceId: String? = null,
    val reason: String? = null,
    val bannedAt: String? = null,
    val bannedBy: String? = null,
    val originalAccount: OriginalAccount? = null,
    val canAppeal: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OriginalAccount(
    val maskedName: String? = null,
    val maskedEmail: String? = null,
    val halaId: String? = null,
    val accountCreatedAt: String? = null,
    val wasSuspended: Boolean = false,
    val wasBanned: Boolean = false
)

// ── الاستئناف ───────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AppealRequest(
    val reason: String,
    val actionType: String
)

@JsonClass(generateAdapter = true)
data class PublicDeviceBanAppealRequest(
    val reason: String,
    val email: String? = null,
    val deviceFingerprint: String? = null,
    val deviceToken: String? = null
)

@JsonClass(generateAdapter = true)
data class AppealResponse(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null
)
