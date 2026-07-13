package com.chathala.hala.feature.userprofile.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val data: UserProfileWrapper? = null
)

@JsonClass(generateAdapter = true)
data class UserProfileWrapper(
    val user: UserProfile? = null
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val photos: List<ProfilePhoto> = emptyList(),
    val birthDate: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val bio: String? = null,
    val isOnline: Boolean? = null,
    val lastLogin: String? = null,
    val isPremium: Boolean? = null,
    val isActive: Boolean? = null,
    /** يرجّعه الخادم = true للحساب الموقوف بالكامل (بيانات مقنّعة) → نخفي أزرار التفاعل. */
    val isSuspendedAccount: Boolean? = null,
    val verification: ProfileVerification? = null,
    val distance: Double? = null,
    val joinDate: String? = null,
    val acceptingRequests: Boolean? = null,
    val premiumOnlyRequests: Boolean? = null,
    val likedYou: Boolean? = null
) {
    /** كل الصور بترتيب الـ order + يبدأ بـ profileImage إذا وُجد. */
    val galleryUrls: List<String>
        get() {
            val sorted = photos.sortedBy { it.order ?: 0 }
            val photoUrls = sorted.mapNotNull { it.medium ?: it.original ?: it.thumbnail }
            val main = profileImage?.takeIf { it.isNotBlank() }
            val result = mutableListOf<String>()
            if (main != null) result.add(main)
            photoUrls.forEach { if (it !in result) result.add(it) }
            return result
        }
}

@JsonClass(generateAdapter = true)
data class ProfilePhoto(
    val original: String? = null,
    val medium: String? = null,
    val thumbnail: String? = null,
    val order: Int? = 0
)

@JsonClass(generateAdapter = true)
data class ProfileVerification(
    val isVerified: Boolean = false,
    val status: String? = null
)
