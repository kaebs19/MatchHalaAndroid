package com.chathala.hala.feature.user.data

/**
 * استخراج دفاعي لنموذج User من استجابة /api/auth/me المُحلَّلة كـ Map.
 *
 * السبب: السيرفر (Mongoose) يُضيف حقولاً مُتداخلة (`verification._id`,
 * `bioStatus._id`, إلخ) بأنواع غير متوقّعة. استخدام data class صارم يفشل
 * عشوائياً. التحليل كـ Map مرن يتعامل مع أي تركيب.
 */

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.toUserDomain(): User? {
    val id = str("_id") ?: return null
    val name = str("name") ?: return null
    val email = str("email") ?: return null

    val verificationMap = this["verification"] as? Map<String, Any?>

    return User(
        id = id,
        name = name,
        email = email,
        role = str("role"),
        profileImage = str("profileImage"),
        photos = extractPhotos(this["photos"]),
        birthDate = str("birthDate"),
        gender = str("gender"),
        country = str("country"),
        city = str("city"),
        bio = str("bio"),
        interests = extractStringList(this["interests"]),
        isPremium = bool("isPremium") ?: false,
        isVerified = bool("isVerified")
            ?: (verificationMap?.get("isVerified") as? Boolean)
            ?: false,
        verificationStatus = verificationMap?.get("status") as? String,
        authProvider = str("authProvider"),
        zodiacSign = str("zodiacSign"),
        joinDate = str("joinDate") ?: str("createdAt"),
        mutedConversationIds = extractMutedConversationIds(this["mutedConversations"]),
        blockedUserIds = extractStringList(this["blockedUsers"])
    )
}

/** mutedConversations تأتي كـ List<{conversationId, mutedUntil}>. */
@Suppress("UNCHECKED_CAST")
private fun extractMutedConversationIds(value: Any?): List<String> {
    val list = value as? List<Any?> ?: return emptyList()
    return list.mapNotNull { item ->
        when (item) {
            is String -> item
            is Map<*, *> -> {
                val m = item as Map<String, Any?>
                m["conversationId"] as? String
            }
            else -> null
        }
    }
}

private fun Map<String, Any?>.str(key: String): String? {
    val v = this[key] ?: return null
    return v as? String
}

private fun Map<String, Any?>.bool(key: String): Boolean? {
    return this[key] as? Boolean
}

/** Photos قد تكون List<String> (بعد التحويل) أو List<Map> (خام). نتعامل مع الحالتين. */
@Suppress("UNCHECKED_CAST")
private fun extractPhotos(value: Any?): List<String> {
    val list = value as? List<Any?> ?: return emptyList()
    return list.mapNotNull { item ->
        when (item) {
            is String -> item
            is Map<*, *> -> {
                val m = item as Map<String, Any?>
                (m["medium"] ?: m["original"] ?: m["thumbnail"]) as? String
            }
            else -> null
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun extractStringList(value: Any?): List<String> {
    val list = value as? List<Any?> ?: return emptyList()
    return list.mapNotNull { it as? String }
}
