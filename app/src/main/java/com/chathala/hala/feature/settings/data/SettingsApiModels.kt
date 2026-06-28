package com.chathala.hala.feature.settings.data

import com.squareup.moshi.JsonClass

// ═══════════════════════════════════════════════════
// Privacy
// ═══════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class PrivacySettingsResponse(
    val success: Boolean,
    val data: PrivacySettingsData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class PrivacySettingsData(
    val profileVisibility: String? = null,
    val showLastSeen: Boolean? = null,
    val notificationSound: Boolean? = null,
    val showDistance: Boolean? = null,
    val showAge: Boolean? = null,
    val showCountry: Boolean? = null,
    val stealthMode: Boolean? = null,
    val acceptingRequests: Boolean? = null,
    val premiumOnlyRequests: Boolean? = null,
    val doNotDisturb: DoNotDisturbData? = null,
    val discoveryPaused: DiscoveryPausedData? = null
)

@JsonClass(generateAdapter = true)
data class DoNotDisturbData(
    val enabled: Boolean = false,
    val startHour: Int = 23,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0
)

@JsonClass(generateAdapter = true)
data class DiscoveryPausedData(
    val enabled: Boolean = false,
    val until: String? = null   // ISO 8601 أو null = حتى الاستئناف اليدوي
)

@JsonClass(generateAdapter = true)
data class UpdateDistanceRequest(val showDistance: Boolean)

@JsonClass(generateAdapter = true)
data class UpdateShowAgeRequest(val showAge: Boolean)

@JsonClass(generateAdapter = true)
data class UpdateShowCountryRequest(val showCountry: Boolean)

@JsonClass(generateAdapter = true)
data class UpdateAcceptingRequestsRequest(val acceptingRequests: Boolean)

@JsonClass(generateAdapter = true)
data class UpdatePremiumOnlyRequestsRequest(val premiumOnlyRequests: Boolean)

@JsonClass(generateAdapter = true)
data class UpdateDoNotDisturbRequest(
    val enabled: Boolean,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null
)

@JsonClass(generateAdapter = true)
data class UpdatePauseDiscoveryRequest(
    val enabled: Boolean,
    val durationHours: Int? = null   // null/0 = حتى الاستئناف اليدوي
)

@JsonClass(generateAdapter = true)
data class DoNotDisturbResponse(
    val success: Boolean,
    val data: DoNotDisturbData? = null,
    val message: String? = null
)

// ═══════════════════════════════════════════════════
// Notification Preferences
// ═══════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class NotificationPrefsResponse(
    val success: Boolean,
    val data: NotificationPrefsData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationPrefsData(
    val pushEnabled: Boolean = true,
    val invitations: Boolean = true,
    val messages: Boolean = true,
    val profileVisits: Boolean = true,
    val appAlerts: Boolean = true
)

/** يُرسَل مفتاح واحد فقط في كل تحديث (الباقي null = لا يتغيّر). */
@JsonClass(generateAdapter = true)
data class UpdateNotificationPrefRequest(
    val pushEnabled: Boolean? = null,
    val invitations: Boolean? = null,
    val messages: Boolean? = null,
    val profileVisits: Boolean? = null,
    val appAlerts: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class UpdateStealthRequest(val stealthMode: Boolean)

// ═══════════════════════════════════════════════════
// About
// ═══════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class AboutResponse(
    val success: Boolean,
    val data: AboutData? = null
)

@JsonClass(generateAdapter = true)
data class AboutData(
    val content: String? = null,
    val appName: String? = null,
    val appVersion: String? = null,
    val lastUpdated: String? = null
)

// ═══════════════════════════════════════════════════
// Contact
// ═══════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class ContactResponse(
    val success: Boolean,
    val data: ContactData? = null
)

@JsonClass(generateAdapter = true)
data class ContactData(
    val content: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val websiteUrl: String? = null,
    val socialMedia: SocialMediaData? = null,
    val lastUpdated: String? = null
)

@JsonClass(generateAdapter = true)
data class SocialMediaData(
    val twitter: String? = null,
    val instagram: String? = null,
    val facebook: String? = null,
    val youtube: String? = null,
    val snapchat: String? = null,
    val tiktok: String? = null
)

// ═══════════════════════════════════════════════════
// Change Password
// ═══════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

// ═══════════════════════════════════════════════════
// Content Preferences
// ═══════════════════════════════════════════════════

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class UpdateAllowSensitiveContentRequest(
    val enabled: Boolean
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class SimpleSettingsResponse(
    val success: Boolean,
    val message: String? = null,
    val enabled: Boolean? = null,
    val code: String? = null
)
