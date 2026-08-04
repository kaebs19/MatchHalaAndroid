package com.chathala.hala.feature.chats.data

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * - قائمة المحادثات النشطة + pending count
 * - mark as read (REST — كبديل/إضافة لـ socket)
 * - يحفظ pendingRecentCount في ذاكرة مشتركة للـ badge (مثل NotificationsRepository.unreadCount)
 */
class ConversationsRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    private val _pendingRecentCount = MutableStateFlow(0)
    val pendingRecentCount: StateFlow<Int> = _pendingRecentCount.asStateFlow()

    private val _totalUnread = MutableStateFlow(0)
    val totalUnread: StateFlow<Int> = _totalUnread.asStateFlow()

    suspend fun fetchConversations(
        page: Int = 1,
        limit: Int = 100
    ): NetworkResult<ConversationsData> = safeApiCall {
        // ✅ نطلب المقبولة + المنتهية (cancelled) — تبقى المحادثات الملغاة ظاهرة مع رسائلها.
        // الطلبات (pending) لها endpoint/شاشة مستقلة، والمرفوضة لا تُعرض.
        val resp = api.getConversations(bearer(), page, limit, status = "accepted,cancelled")
        val data = resp.data ?: throw IllegalStateException(S.get(R.string.err_data_unavailable))
        _totalUnread.value = data.totalUnread
        data
    }

    /** جلب محادثة واحدة (للطرف الآخر عند فتح الشات) — أخفّ بكثير من جلب القائمة كاملة. */
    suspend fun fetchConversation(conversationId: String): NetworkResult<Conversation> = safeApiCall {
        val resp = api.getConversation(bearer(), conversationId)
        resp.data?.conversation ?: throw IllegalStateException(S.get(R.string.err_conversation_unavailable))
    }

    suspend fun refreshPendingCount(): NetworkResult<PendingCountData> = safeApiCall {
        val resp = api.getPendingCount(bearer())
        val data = resp.data ?: PendingCountData()
        _pendingRecentCount.value = data.recent
        data
    }

    suspend fun fetchPendingRequests(): NetworkResult<PendingRequestsData> = safeApiCall {
        val resp = api.getPendingRequests(bearer())
        val data = resp.data ?: throw IllegalStateException(S.get(R.string.err_data_unavailable))
        _pendingRecentCount.value = data.recentCount
        data
    }

    suspend fun acceptRequest(conversationId: String): NetworkResult<String> = safeApiCall {
        val resp = api.acceptConversation(bearer(), conversationId)
        resp.message ?: S.get(R.string.conv_accepted)
    }

    suspend fun acceptRequestWithMessage(
        conversationId: String,
        greeting: String?
    ): NetworkResult<AcceptWithMessageData> = safeApiCall {
        val resp = api.acceptWithMessage(
            bearer = bearer(),
            id = conversationId,
            body = AcceptWithMessageRequest(greeting = greeting?.takeIf { it.isNotBlank() })
        )
        resp.data ?: AcceptWithMessageData(conversationId = conversationId)
    }

    suspend fun rejectRequest(conversationId: String): NetworkResult<String> = safeApiCall {
        val resp = api.rejectConversation(bearer(), conversationId)
        resp.message ?: S.get(R.string.conv_rejected)
    }

    suspend fun markRead(conversationId: String): NetworkResult<String> = safeApiCall {
        val resp = api.markConversationRead(bearer(), conversationId)
        resp.message ?: S.get(R.string.conv_updated)
    }

    suspend fun deleteConversation(conversationId: String): NetworkResult<String> = safeApiCall {
        val resp = api.deleteConversation(bearer(), conversationId)
        resp.message ?: S.get(R.string.conv_deleted)
    }

    suspend fun cancelConversation(conversationId: String): NetworkResult<String> = safeApiCall {
        val resp = api.cancelConversation(bearer(), conversationId)
        resp.message ?: S.get(R.string.conv_ended)
    }

    /** إرسال طلب محادثة جديد (يُستخدم لاستئناف محادثة منتهية). */
    suspend fun requestConversation(targetUserId: String): NetworkResult<String> = safeApiCall {
        val resp = api.requestConversation(
            bearer = bearer(),
            body = com.chathala.hala.feature.discover.data.RequestConversationRequest(
                targetUserId = targetUserId,
                initialMessage = null,
                isSuperLike = false
            )
        )
        resp.message ?: S.get(R.string.conv_request_sent)
    }

    suspend fun setMute(
        conversationId: String,
        muted: Boolean,
        mutedUntilIso: String? = null
    ): NetworkResult<Boolean> = safeApiCall {
        val resp = api.muteConversation(
            bearer = bearer(),
            id = conversationId,
            body = MuteRequest(muted = muted, mutedUntil = mutedUntilIso)
        )
        resp.muted ?: muted
    }

    suspend fun fetchChatMode(conversationId: String): NetworkResult<String> = safeApiCall {
        val resp = api.getChatMode(bearer(), conversationId)
        resp.data?.chatMode ?: "snap"
    }

    suspend fun setChatMode(
        conversationId: String,
        mode: String   // snap | 24h | keep
    ): NetworkResult<ChatModeData> = safeApiCall {
        val resp = api.setChatMode(
            bearer = bearer(),
            id = conversationId,
            body = ChatModeRequest(chatMode = mode)
        )
        resp.data ?: throw IllegalStateException(S.get(R.string.conv_mode_change_failed))
    }

    fun decrementUnread(by: Int) {
        _totalUnread.value = (_totalUnread.value - by).coerceAtLeast(0)
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException(S.get(R.string.auth_no_active_session))
        return "Bearer $token"
    }
}
