package com.chathala.hala.feature.stories.data

import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.chats.socket.SocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * حالة القصص على مستوى التطبيق (مقابل `StoryManager` في iOS):
 *  - قصصي النشطة والحدّ ([myStories], [limits])
 *  - حالات الحلقات لكل مستخدم ([statuses]) بطلب مجمّع مع منع التكرار خلال 60ث
 *  - المكتومون محلياً (يُزالون من الحلقات فوراً)
 *  - عدّاد الإعلان بين القصص عبر الجلسة
 *
 * يُمسح كله عند تسجيل الخروج ([reset]).
 */
class StoryManager(
    private val repo: StoryRepository,
    socketEvents: Flow<SocketEvent>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    data class Limits(val max: Int = FREE_MAX, val freeMax: Int = FREE_MAX, val premiumMax: Int = PREMIUM_MAX)

    /** رفع جارٍ — بطاقة بحلقة تقدّم في شريط «قصصي». */
    data class Upload(val id: Long, val progress: Float)

    private val _myStories = MutableStateFlow<List<Story>>(emptyList())
    val myStories: StateFlow<List<Story>> = _myStories.asStateFlow()

    private val _myLoaded = MutableStateFlow(false)
    val myLoaded: StateFlow<Boolean> = _myLoaded.asStateFlow()

    private val _limits = MutableStateFlow(Limits())
    val limits: StateFlow<Limits> = _limits.asStateFlow()

    private val _uploads = MutableStateFlow<List<Upload>>(emptyList())
    val uploads: StateFlow<List<Upload>> = _uploads.asStateFlow()

    private val _statuses = MutableStateFlow<Map<String, StoryStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, StoryStatus>> = _statuses.asStateFlow()

    private val _muted = MutableStateFlow<Set<String>>(emptySet())
    val muted: StateFlow<Set<String>> = _muted.asStateFlow()

    /** إشعار لحظي للعارض المفتوح: (storyId, viewCount, screenshotCount). */
    private val _counterUpdates = MutableSharedFlow<Story>(extraBufferCapacity = 16)
    val counterUpdates: SharedFlow<Story> = _counterUpdates.asSharedFlow()

    private val statusMutex = Mutex()
    private val lastStatusFetch = HashMap<String, Long>()
    private var pruneJob: Job? = null

    /** قصص الآخرين المعروضة عبر الجلسة — كل [AD_EVERY] يُعرض إعلان. */
    private var othersViewedSinceAd = 0

    init {
        socketEvents.onEach { event ->
            when (event) {
                is SocketEvent.StoryViewed -> updateCounter(
                    event.json.optString("storyId"),
                    viewCount = event.json.optInt("viewCount", -1).takeIf { it >= 0 }
                )
                is SocketEvent.StoryReaction -> updateCounter(
                    event.json.optString("storyId"),
                    // null = تبديل إيموجي لا يغيّر العدد
                    reactionCount = event.json.optInt("reactionCount", -1).takeIf { it >= 0 && !event.json.isNull("reactionCount") }
                )
                is SocketEvent.StoryScreenshot -> updateCounter(
                    event.json.optString("storyId"),
                    screenshotCount = event.json.optInt("screenshotCount", -1).takeIf { it >= 0 }
                )
                else -> Unit
            }
        }.launchIn(scope)
    }

    // ── قصصي ─────────────────────────────────────────────────────

    suspend fun refreshMine(): NetworkResult<MyStoriesData> {
        val r = repo.myStories()
        if (r is NetworkResult.Success) {
            _myStories.value = r.data.stories.filterNot { it.isExpired() }.sortedBy { it.createdAtMillis ?: 0L }
            _limits.value = Limits(r.data.max, r.data.freeMax, r.data.premiumMax)
            _myLoaded.value = true
        }
        startPruning()
        return r
    }

    /** يُستدعى عند تغيّر الاشتراك — المشترك الجديد كان يبقى عند 3. */
    fun onPremiumChanged() {
        scope.launch { refreshMine() }
    }

    val remainingSlots: Int get() = (_limits.value.max - _myStories.value.size).coerceAtLeast(0)

    suspend fun publishImage(jpeg: ByteArray, locationTag: StoryLocationTag? = null, caption: String? = null): NetworkResult<StoryPublishData> {
        val uploadId = System.nanoTime()
        _uploads.update { it + Upload(uploadId, 0f) }
        // الرفع صغير (≤1MB) — تقدّم تقريبي يكفي للحلقة
        val ticker = scope.launch {
            var p = 0f
            while (isActive && p < 0.9f) {
                delay(120); p += 0.06f
                _uploads.update { list -> list.map { if (it.id == uploadId) it.copy(progress = p) else it } }
            }
        }
        val r = repo.publishImage(jpeg, locationTag, caption)
        ticker.cancel()
        _uploads.update { list -> list.filterNot { it.id == uploadId } }
        if (r is NetworkResult.Success) onPublished(r.data)
        return r
    }

    suspend fun publishText(text: String, background: Int): NetworkResult<StoryPublishData> {
        val r = repo.publishText(text, background)
        if (r is NetworkResult.Success) onPublished(r.data)
        return r
    }

    private fun onPublished(data: StoryPublishData) {
        _myStories.update { (it.filterNot { s -> s.id == data.story.id } + data.story) }
        _limits.value = Limits(data.max, data.freeMax, data.premiumMax)
        startPruning()
    }

    suspend fun delete(storyId: String): NetworkResult<Unit> {
        val r = repo.delete(storyId)
        if (r is NetworkResult.Success || (r as? NetworkResult.Error)?.httpCode == 404) {
            _myStories.update { list -> list.filterNot { it.id == storyId } }
        }
        return r
    }

    private fun updateCounter(storyId: String, viewCount: Int? = null, screenshotCount: Int? = null, reactionCount: Int? = null) {
        if (storyId.isBlank()) return
        var updated: Story? = null
        _myStories.update { list ->
            list.map {
                if (it.id == storyId) {
                    it.copy(
                        viewCount = viewCount ?: it.viewCount,
                        screenshotCount = screenshotCount ?: it.screenshotCount,
                        reactionCount = reactionCount ?: it.reactionCount
                    ).also { s -> updated = s }
                } else it
            }
        }
        updated?.let { _counterUpdates.tryEmit(it) }
    }

    /** كنس المنتهية محلياً كل دقيقة — الخادم يكنس كل 5 دقائق. */
    private fun startPruning() {
        if (pruneJob?.isActive == true) return
        pruneJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _myStories.update { list -> list.filterNot { it.isExpired(now) } }
                delay(60_000)
            }
        }
    }

    // ── قصص الآخرين ──────────────────────────────────────────────

    suspend fun userStories(userId: String): NetworkResult<UserStoriesData> {
        val r = repo.userStories(userId)
        if (r is NetworkResult.Success) {
            val stories = r.data.stories.filterNot { it.isExpired() }
            if (r.data.muted) _muted.update { it + userId } else _muted.update { it - userId }
            _statuses.update { map ->
                if (stories.isEmpty()) map - userId
                else map + (userId to StoryStatus(stories.size, stories.any { it.viewed != true }))
            }
        }
        return r
    }

    fun hasStories(userId: String?): Boolean =
        userId != null && userId !in _muted.value && (_statuses.value[userId]?.count ?: 0) > 0

    /**
     * يجلب حالات الحلقات لمجموعة مستخدمين. لا يكرّر الطلب لنفس المجموعة خلال
     * 60 ثانية (iOS كان يطلقه مع كل تبديل تبويب). [force] للسحب للتحديث.
     */
    fun loadStatuses(userIds: Collection<String>, batchSize: Int = STATUS_MAX, force: Boolean = false) {
        val ids = userIds.filter { it.isNotBlank() }.distinct().take(STATUS_MAX)
        if (ids.isEmpty()) return
        scope.launch {
            statusMutex.withLock {
                val key = ids.sorted().joinToString(",").hashCode().toString()
                val now = System.currentTimeMillis()
                if (!force && (lastStatusFetch[key]?.let { now - it < STATUS_TTL_MS } == true)) return@withLock
                lastStatusFetch[key] = now
                for (chunk in ids.chunked(batchSize)) {
                    val r = repo.statuses(chunk)
                    if (r is NetworkResult.Success) {
                        _statuses.update { map -> map - chunk.toSet() + r.data }
                    } else {
                        lastStatusFetch.remove(key)
                        return@withLock
                    }
                }
            }
        }
    }

    /** بعد مشاهدة قصة: إن شوهدت كلها تصير الحلقة رمادية. */
    fun markViewed(userId: String, allViewed: Boolean) {
        if (!allViewed) return
        _statuses.update { map ->
            map[userId]?.let { map + (userId to it.copy(hasUnseen = false)) } ?: map
        }
    }

    suspend fun setMuted(userId: String, muted: Boolean): NetworkResult<Boolean> {
        val r = repo.setMuted(userId, muted)
        if (r is NetworkResult.Success) {
            _muted.update { if (r.data) it + userId else it - userId }
            if (!r.data) lastStatusFetch.clear()
        }
        return r
    }

    fun recordView(storyId: String) {
        scope.launch { repo.view(storyId) }
    }

    fun recordScreenshot(storyId: String) {
        scope.launch { repo.screenshot(storyId) }
    }

    suspend fun viewers(storyId: String) = repo.viewers(storyId)

    /** خلاصة «القصص»: كل من له قصة ممن تعرفهم، بترتيب الخادم. */
    suspend fun feed() = repo.feed()

    suspend fun react(storyId: String, emoji: String?) = repo.react(storyId, emoji)

    // ── الإعلان بين القصص ────────────────────────────────────────

    /** تُستدعى عند ظهور قصة لغيري؛ تُرجع true إن حان وقت إعلان قبل التالية. */
    fun countOthersStoryShown(): Boolean {
        othersViewedSinceAd++
        return othersViewedSinceAd >= AD_EVERY
    }

    fun adShown() {
        othersViewedSinceAd = 0
    }

    // ── الجلسة ───────────────────────────────────────────────────

    fun reset() {
        pruneJob?.cancel()
        pruneJob = null
        _myStories.value = emptyList()
        _myLoaded.value = false
        _limits.value = Limits()
        _uploads.value = emptyList()
        _statuses.value = emptyMap()
        _muted.value = emptySet()
        lastStatusFetch.clear()
        othersViewedSinceAd = 0
    }

    companion object {
        const val STATUS_MAX = 200
        const val DISCOVER_BATCH = 40
        const val STATUS_TTL_MS = 60_000L
        const val AD_EVERY = 5
        const val STORY_DURATION_MS = 5_000L
    }
}
