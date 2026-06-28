package com.chathala.hala.feature.discover.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

/**
 * جلب بطاقات الاستكشاف + إرسال طلب محادثة.
 *
 * الباك إند:
 *  - GET /api/swipes/cards (مع فلاتر)
 *  - POST /api/mobile/conversations/request
 */
class DiscoverRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    data class Filters(
        val gender: String? = null,           // male / female
        val minAge: Int? = null,
        val maxAge: Int? = null,
        val onlyRecent: Boolean = false       // آخر 7 أيام
    )

    suspend fun fetchCards(
        page: Int = 1,
        limit: Int = 100,
        filters: Filters = Filters(),
        latitude: Double? = null,
        longitude: Double? = null
    ): NetworkResult<DiscoverCardsData> = safeApiCall {
        val resp = api.getDiscoverCards(
            bearer = bearer(),
            page = page,
            limit = limit,
            gender = filters.gender,
            minAge = filters.minAge,
            maxAge = filters.maxAge,
            lastActiveWithin = if (filters.onlyRecent) "7d" else null,
            latitude = latitude,
            longitude = longitude
        )
        resp.data ?: throw IllegalStateException("بيانات غير متوفرة")
    }

    /** يُرجع conversationId (جديد أو موجود) — ليفتح المستخدم المحادثة بعدها. */
    suspend fun requestConversation(
        targetUserId: String,
        initialMessage: String? = null,
        isSuperLike: Boolean = false
    ): NetworkResult<RequestResult> = safeApiCall {
        val resp = api.requestConversation(
            bearer = bearer(),
            body = RequestConversationRequest(
                targetUserId = targetUserId,
                initialMessage = initialMessage?.takeIf { it.isNotBlank() },
                isSuperLike = isSuperLike
            )
        )
        RequestResult(
            conversationId = resp.data?.conversation?.id,
            isExisting = resp.data?.isExisting ?: false,
            isSuperLike = resp.data?.isSuperLike ?: false,
            message = resp.message
        )
    }

    data class RequestResult(
        val conversationId: String?,
        val isExisting: Boolean,
        val isSuperLike: Boolean,
        val message: String?
    )

    /**
     * يسجّل إعجاباً/تخطّياً (swipe) — لا يُنشئ طلب محادثة.
     * المحادثة تُنشأ تلقائياً فقط عند **تطابق متبادل** (matched = true).
     */
    suspend fun recordSwipe(userId: String, type: String): NetworkResult<SwipeResult> = safeApiCall {
        val resp = api.swipe(bearer(), SwipeRequest(userId = userId, type = type))
        SwipeResult(
            matched = resp.data?.match != null,
            conversationId = resp.data?.match?.conversationId,
            message = resp.message
        )
    }

    data class SwipeResult(
        val matched: Boolean,
        val conversationId: String?,
        val message: String?
    )

    /** بحث عن مستخدمين بالاسم أو المعرّف (مع pagination + فلاتر). الخادم يتطلب طول q ≥ 2. */
    suspend fun searchUsers(
        query: String,
        page: Int = 1,
        gender: String? = null,
        country: String? = null,
        minAge: Int? = null,
        maxAge: Int? = null
    ): NetworkResult<UserSearchData> = safeApiCall {
        val resp = api.searchUsers(
            bearer(), q = query, page = page,
            gender = gender, country = country, minAge = minAge, maxAge = maxAge
        )
        resp.data ?: UserSearchData()
    }

    /**
     * اقتراحات صفحة البحث: المشتركون أو المتصلون (بدون نص بحث).
     * @param random عيّنة عشوائية (تنويع كل دخول + دفعات «تحميل المزيد»).
     */
    suspend fun suggestedUsers(
        isPremium: Boolean? = null,
        online: Boolean? = null,
        gender: String? = null,
        country: String? = null,
        minAge: Int? = null,
        maxAge: Int? = null,
        random: Boolean = false,
        limit: Int = 12
    ): NetworkResult<UserSearchData> = safeApiCall {
        val resp = api.searchUsers(
            bearer = bearer(),
            q = null,
            limit = limit,
            isPremium = isPremium,
            online = online,
            gender = gender,
            country = country,
            minAge = minAge,
            maxAge = maxAge,
            random = if (random) true else null
        )
        resp.data ?: UserSearchData()
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
