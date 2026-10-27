package com.chathala.hala.feature.stories.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * قصص البروفايل — مسارات `/api/mobile/stories*` (منشورة على الخادم).
 * صورة أو نصّ على خلفية، تختفي بعد 24 ساعة، 3 نشطة مجاناً و10 لهلا+.
 */
@JsonClass(generateAdapter = true)
data class Story(
    @Json(name = "_id") val id: String,
    val kind: String = KIND_IMAGE,
    val imageUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val text: String? = null,
    val background: Int? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    // لصاحبها فقط
    val viewCount: Int? = null,
    val screenshotCount: Int? = null,
    val held: Boolean? = null,
    // للمشاهد فقط
    val viewed: Boolean? = null
) {
    val isText: Boolean get() = kind == KIND_TEXT
    val isHeld: Boolean get() = held == true

    val expiresAtMillis: Long? get() = expiresAt.toEpochMillis()
    val createdAtMillis: Long? get() = createdAt.toEpochMillis()

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        expiresAtMillis?.let { it <= now } ?: false

    companion object {
        const val KIND_IMAGE = "image"
        const val KIND_TEXT = "text"
    }
}

internal fun String?.toEpochMillis(): Long? =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

/** غلاف ردّ عام `{ success, data, message, code }`. */
@JsonClass(generateAdapter = true)
data class StoryEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class StoryPublishData(
    val story: Story,
    val active: Int = 0,
    val max: Int = FREE_MAX,
    val freeMax: Int = FREE_MAX,
    val premiumMax: Int = PREMIUM_MAX
)

@JsonClass(generateAdapter = true)
data class MyStoriesData(
    val stories: List<Story> = emptyList(),
    val max: Int = FREE_MAX,
    val freeMax: Int = FREE_MAX,
    val premiumMax: Int = PREMIUM_MAX
)

@JsonClass(generateAdapter = true)
data class UserStoriesData(
    val stories: List<Story> = emptyList(),
    val muted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TextStoryRequest(val text: String, val background: Int)

@JsonClass(generateAdapter = true)
data class StoryStatusRequest(val userIds: List<String>)

@JsonClass(generateAdapter = true)
data class StoryStatus(val count: Int = 0, val hasUnseen: Boolean = false)

@JsonClass(generateAdapter = true)
data class StoryStatusesData(val statuses: Map<String, StoryStatus> = emptyMap())

@JsonClass(generateAdapter = true)
data class StoryRecordedData(val recorded: Boolean = false)

@JsonClass(generateAdapter = true)
data class StoryMuteData(val muted: Boolean = false)

@JsonClass(generateAdapter = true)
data class StoryViewerUser(
    @Json(name = "_id") val id: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val country: String? = null,
    val isVerified: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class StoryViewerItem(
    val viewer: StoryViewerUser? = null,
    val viewedAt: String? = null,
    val screenshotAt: String? = null
)

@JsonClass(generateAdapter = true)
data class StoryViewersData(
    val viewCount: Int = 0,
    val screenshotCount: Int = 0,
    val viewers: List<StoryViewerItem> = emptyList(),
    val isPremiumRequired: Boolean = false
)

/** `data` لرمز `STORY_LIMIT`. */
@JsonClass(generateAdapter = true)
data class StoryLimitData(
    val max: Int = FREE_MAX,
    val active: Int = 0,
    val freeMax: Int = FREE_MAX,
    val premiumMax: Int = PREMIUM_MAX,
    val isPremium: Boolean = false
)

const val FREE_MAX = 3
const val PREMIUM_MAX = 10
const val STORY_TEXT_MAX = 250

/** رموز أخطاء القصص من الخادم. */
object StoryErrorCodes {
    const val LIMIT = "STORY_LIMIT"
    const val BANNED = "STORY_BANNED"
    const val FORBIDDEN = "STORY_FORBIDDEN"
    const val TEXT_BLOCKED = "STORY_TEXT_BLOCKED"
    const val TOO_LARGE = "STORY_TOO_LARGE"
    const val INVALID = "STORY_INVALID"
    const val NOT_FOUND = "STORY_NOT_FOUND"
}
