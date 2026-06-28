package com.chathala.hala.feature.user.data

import com.squareup.moshi.JsonClass

/**
 * نموذج المستخدم المحلي (domain) — مخزّن في UserStorage كـ JSON.
 * يُعاد بناؤه من AuthResponse أو /api/auth/me.
 * Nullable fields تُملأ تدريجياً (login يعطي الأساسي، /me يعطي الكامل).
 */
@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String? = null,
    val profileImage: String? = null,
    val photos: List<String> = emptyList(),
    val birthDate: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val city: String? = null,
    val bio: String? = null,
    val interests: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val isVerified: Boolean = false,
    val verificationStatus: String? = null, // none | pending | rejected | verified
    val authProvider: String? = null,
    val zodiacSign: String? = null,
    val joinDate: String? = null,
    val mutedConversationIds: List<String> = emptyList(),
    val blockedUserIds: List<String> = emptyList()
)
