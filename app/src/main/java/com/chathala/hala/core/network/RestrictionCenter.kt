package com.chathala.hala.core.network

import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** تقييد المراسلة كما تعلنه ترويسات الخادم `X-Restriction-*` في كل ردّ مصادَق. */
data class HeaderRestriction(
    val level: String,          // new_only | all
    val untilMillis: Long?,
    val reason: String?
) {
    val isExternalPromo: Boolean get() = reason?.contains("external_promotion", ignoreCase = true) == true
    /** إيقاف بدء المحادثات الجديدة بسبب الحسابات الخارجية — المحادثات القائمة تعمل. */
    val isNewChatPause: Boolean get() = isExternalPromo && level == "new_only"
    fun active(now: Long = System.currentTimeMillis()): Boolean = untilMillis == null || untilMillis > now
}

/**
 * مركز حالة التقييد:
 * - [header]: آخر ما أعلنته ترويسات الخادم (يُصفَّر عند ردّ مصادَق ناجح بلا ترويسة).
 * - [newChatPauseUntil]: يُضبط حين يُرفض بدء محادثة/سوبر لايك بـ 403 `MESSAGING_RESTRICTED`
 *   بسبب `external_promotion` ومستوى `new_only` — يعرض حواراً يبقى حتى «حسناً» (مقابل
 *   `NewChatPauseAlert` في iOS) بدل Toast يختفي.
 */
object RestrictionCenter {
    private val _header = MutableStateFlow<HeaderRestriction?>(null)
    val header: StateFlow<HeaderRestriction?> = _header.asStateFlow()

    private val _newChatPauseUntil = MutableStateFlow<Long?>(null)
    val newChatPauseUntil: StateFlow<Long?> = _newChatPauseUntil.asStateFlow()

    fun dismissNewChatPause() { _newChatPauseUntil.value = null }

    /**
     * يُستدعى من ViewModel عند فشل طلب محادثة/سوبر لايك: إن كان الرفض إيقاف الحسابات
     * الخارجية يعرض الحوار ويُرجع true — فلا يكرّره المستدعي بـ Toast.
     */
    fun consumeNewChatPause(error: NetworkResult.Error): Boolean {
        if (error.code != "MESSAGING_RESTRICTED") return false
        val data = (error.payload as? com.chathala.hala.feature.auth.data.ErrorBody)?.data ?: return false
        if (data.reason?.contains("external_promotion") != true) return false
        if (data.level?.toString() != "new_only") return false
        val until = parseServerIso(data.until) ?: return false
        if (until <= System.currentTimeMillis()) return false
        _newChatPauseUntil.value = until
        return true
    }

    internal fun publishHeader(value: HeaderRestriction?) {
        if (_header.value != value) _header.value = value
    }

    /** مدة بالدقائق بصيغة مقروءة: «10 دقائق» / «ساعتان» / «3 أيام». */
    fun formatDuration(minutes: Int): String {
        val m = minutes.coerceAtLeast(1)
        return when {
            m < 60 -> com.chathala.hala.core.i18n.S.plural(com.chathala.hala.R.plurals.duration_minutes, m)
            m < 24 * 60 -> com.chathala.hala.core.i18n.S.plural(com.chathala.hala.R.plurals.duration_hours, (m + 59) / 60)
            else -> com.chathala.hala.core.i18n.S.plural(com.chathala.hala.R.plurals.duration_days, m / (24 * 60))
        }
    }

    /** المتبقّي حتى [untilMillis] مقرّباً لأعلى بالدقيقة. */
    fun formatRemaining(untilMillis: Long): String =
        formatDuration(((untilMillis - System.currentTimeMillis() + 59_999L) / 60_000L).toInt())

    /** ISO من Node بأجزاء الثانية أو بدونها → millis. */
    fun parseServerIso(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return runCatching { java.time.Instant.parse(iso).toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
    }
}

/** يقرأ `X-Restriction-Level/Until/Reason` من كل ردّ لطلب مصادَق. */
class RestrictionHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.header("Authorization").isNullOrBlank()) return response
        val level = response.header("X-Restriction-Level")
        when {
            !level.isNullOrBlank() && level != "none" -> RestrictionCenter.publishHeader(
                HeaderRestriction(
                    level = level,
                    untilMillis = RestrictionCenter.parseServerIso(response.header("X-Restriction-Until")),
                    reason = response.header("X-Restriction-Reason")
                )
            )
            // الخادم لا يرسل الترويسة لغير المقيَّد — ردّ ناجح بلا ترويسة = لا تقييد
            response.isSuccessful -> RestrictionCenter.publishHeader(null)
        }
        return response
    }
}
