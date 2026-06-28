package com.chathala.hala.feature.chats.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.blocking.data.BlockingRepository
import com.chathala.hala.feature.chats.data.ChatsCacheStorage
import com.chathala.hala.feature.chats.data.Conversation
import com.chathala.hala.feature.chats.data.ConversationsRepository
import com.chathala.hala.feature.chats.socket.HalaSocket
import com.chathala.hala.feature.chats.socket.SocketEvent
import com.chathala.hala.feature.reporting.data.ReportReason
import com.chathala.hala.feature.reporting.data.ReportRepository
import com.chathala.hala.feature.user.data.User
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatsFilter { ALL, UNREAD, PINNED, PREMIUM }

data class ChatsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val all: List<Conversation> = emptyList(),
    val pinnedIds: Set<String> = emptySet(),
    val pendingBadge: Int = 0,
    val currentUserId: String? = null,
    val socketConnected: Boolean = false,
    val mutedConversationIds: Set<String> = emptySet(),
    val filter: ChatsFilter = ChatsFilter.ALL,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val actionTarget: Conversation? = null,
    val processingIds: Set<String> = emptySet()
) {
    /**
     * المحادثات الظاهرة في القائمة الرئيسية: المقبولة فقط.
     * الطلبات (مستلمة/مرسلة) تظهر فقط في شاشة الطلبات.
     */
    val accepted: List<Conversation>
        get() = all.filter { it.status == "accepted" }

    val unreadCount: Int get() = accepted.sumOf { it.unreadCount }

    /** القائمة بعد تطبيق الفلتر + البحث، مرتّبة بالمثبّتة أولاً. */
    val visible: List<Conversation>
        get() {
            val base = when (filter) {
                ChatsFilter.ALL -> accepted
                ChatsFilter.UNREAD -> accepted.filter { it.unreadCount > 0 }
                ChatsFilter.PINNED -> accepted.filter { it.id in pinnedIds }
                ChatsFilter.PREMIUM -> accepted.filter { conv ->
                    conv.participants.any { p -> p.id != currentUserId && p.isPremium == true }
                }
            }
            val filtered = if (searchQuery.isBlank()) base else {
                val q = searchQuery.trim().lowercase()
                base.filter { conv ->
                    val other = conv.participants.firstOrNull { it.id != currentUserId }
                    val nameMatch = other?.name?.lowercase()?.contains(q) == true
                    val msgMatch =
                        conv.lastMessage?.content?.lowercase()?.contains(q) == true
                    nameMatch || msgMatch
                }
            }
            // المثبّتة أولاً
            return filtered.sortedByDescending { it.id in pinnedIds }
        }
}

class ChatsViewModel(
    private val repo: ConversationsRepository,
    private val userRepo: UserRepository,
    private val socket: HalaSocket,
    private val cache: ChatsCacheStorage,
    private val blocking: BlockingRepository,
    private val reporting: ReportRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    // غرف المحادثات التي انضممنا إليها على القائمة (لاستقبال new-message لحظياً)
    // يجب أن تُهيّأ قبل كتلة init لأن مراقب الاتصال يستخدمها فوراً
    private val joinedRooms = mutableSetOf<String>()

    init {
        userRepo.currentUser
            .onEach { u: User? ->
                _state.update {
                    it.copy(
                        currentUserId = u?.id,
                        mutedConversationIds = u?.mutedConversationIds?.toSet() ?: emptySet()
                    )
                }
            }
            .launchIn(viewModelScope)

        socket.connected
            .onEach { c ->
                _state.update { it.copy(socketConnected = c) }
                // عند إعادة الاتصال: الغرف تُفقد على الخادم — أعد الانضمام + اجلب حالة محدّثة
                if (c) {
                    joinedRooms.clear()
                    joinConversationRooms(_state.value.all)
                    refresh(silent = true)   // حالة اتصال/رسائل محدّثة بعد أي انقطاع
                }
            }
            .launchIn(viewModelScope)

        socket.incoming
            .onEach { evt ->
                when (evt) {
                    is SocketEvent.NewMessage -> {
                        applyIncomingMessage(evt.json)   // تحديث فوري محلي
                        refresh(silent = true)            // مصالحة مع الخادم
                    }
                    is SocketEvent.ConversationAccepted,
                    is SocketEvent.ConversationRejected,
                    is SocketEvent.ConversationRequest -> refresh(silent = true)
                    // تحديث حالة الاتصال فورياً دون إعادة جلب
                    is SocketEvent.UserOnline -> updateParticipantPresence(evt.json, online = true)
                    is SocketEvent.UserOffline -> updateParticipantPresence(evt.json, online = false)
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)

        // متابعة المحادثات المثبّتة من DataStore
        cache.pinnedIds
            .onEach { ids -> _state.update { it.copy(pinnedIds = ids) } }
            .launchIn(viewModelScope)

        load()
        refreshPendingBadge()
    }

    fun load() {
        viewModelScope.launch {
            val cached = runCatching { cache.readConversations() }.getOrNull()
            if (!cached.isNullOrEmpty()) {
                _state.update {
                    it.copy(loading = false, all = cached, error = null, refreshing = true)
                }
            } else {
                _state.update { it.copy(loading = true, error = null) }
            }
            fetch(silent = true)
        }
    }

    fun refresh(silent: Boolean = false) {
        if (_state.value.refreshing) return
        if (!silent) _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            fetch(silent = silent)
            refreshPendingBadge()
        }
    }

    /** تحديث فوري للقائمة عند وصول رسالة عبر السوكت: آخر رسالة + عدّاد غير المقروء + قفز للأعلى. */
    private fun applyIncomingMessage(json: org.json.JSONObject) {
        val msg = json.optJSONObject("message") ?: json
        val convId = msg.optString("conversation").takeIf { it.isNotBlank() } ?: return
        val senderId = msg.optJSONObject("sender")?.optString("_id")?.takeIf { it.isNotBlank() }
            ?: msg.optString("sender").takeIf { it.isNotBlank() }
        val isMine = senderId != null && senderId == _state.value.currentUserId
        val newLast = com.chathala.hala.feature.chats.data.LastMessage(
            id = msg.optString("_id").takeIf { it.isNotBlank() },
            content = msg.optString("content").takeIf { it.isNotBlank() },
            type = msg.optString("type").takeIf { it.isNotBlank() },
            sender = senderId,
            isRead = false,
            createdAt = msg.optString("createdAt").takeIf { it.isNotBlank() }
        )
        _state.update { s ->
            val idx = s.all.indexOfFirst { it.id == convId }
            if (idx < 0) return@update s   // محادثة جديدة — يتكفّل بها refresh
            val updated = s.all[idx].copy(
                lastMessage = newLast,
                unreadCount = if (isMine) s.all[idx].unreadCount else s.all[idx].unreadCount + 1
            )
            s.copy(all = listOf(updated) + s.all.filterIndexed { i, _ -> i != idx })
        }
    }

    /** يحدّث حالة اتصال مشارِك في كل المحادثات عند وصول حدث user:online/offline. */
    private fun updateParticipantPresence(json: org.json.JSONObject, online: Boolean) {
        val userId = json.optString("userId").takeIf { it.isNotBlank() } ?: return
        _state.update { s ->
            s.copy(
                all = s.all.map { conv ->
                    if (conv.participants.any { it.id == userId }) {
                        conv.copy(
                            participants = conv.participants.map { p ->
                                if (p.id == userId) p.copy(isOnline = online) else p
                            }
                        )
                    } else conv
                }
            )
        }
    }

    fun refreshPendingBadge() {
        viewModelScope.launch {
            val r = repo.refreshPendingCount()
            if (r is NetworkResult.Success) {
                _state.update { it.copy(pendingBadge = r.data.recent) }
            }
        }
    }

    // ── Filters / Search ──────────────────────────────────────────

    fun setFilter(filter: ChatsFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun toggleSearch() {
        _state.update {
            it.copy(searchOpen = !it.searchOpen, searchQuery = if (it.searchOpen) "" else it.searchQuery)
        }
    }

    fun setSearchQuery(q: String) {
        _state.update { it.copy(searchQuery = q) }
    }

    // ── Long-press actions sheet ──────────────────────────────────

    fun openActions(conversation: Conversation) {
        _state.update { it.copy(actionTarget = conversation) }
    }

    fun dismissActions() {
        _state.update { it.copy(actionTarget = null) }
    }

    // ── Pin ───────────────────────────────────────────────────────

    fun togglePin(conversationId: String) {
        val isPinned = conversationId in _state.value.pinnedIds
        viewModelScope.launch {
            cache.setPinned(conversationId, !isPinned)
            _message.tryEmit(if (isPinned) "تم إلغاء التثبيت" else "تم تثبيت المحادثة")
        }
        _state.update { it.copy(actionTarget = null) }
    }

    // ── Mute (REST + refresh user) ────────────────────────────────

    fun toggleMute(conversationId: String) {
        val muted = conversationId in _state.value.mutedConversationIds
        _state.update { it.copy(actionTarget = null, processingIds = it.processingIds + conversationId) }
        viewModelScope.launch {
            val r = repo.setMute(conversationId, muted = !muted, mutedUntilIso = null)
            _state.update { it.copy(processingIds = it.processingIds - conversationId) }
            when (r) {
                is NetworkResult.Success -> {
                    _message.tryEmit(if (muted) "تم إلغاء الكتم" else "تم كتم المحادثة")
                    runCatching { userRepo.refresh() }
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────

    fun deleteConversation(conversationId: String) {
        _state.update { it.copy(actionTarget = null, processingIds = it.processingIds + conversationId) }
        viewModelScope.launch {
            when (val r = repo.deleteConversation(conversationId)) {
                is NetworkResult.Success -> {
                    _state.update { s ->
                        s.copy(
                            all = s.all.filterNot { it.id == conversationId },
                            processingIds = s.processingIds - conversationId
                        )
                    }
                    runCatching { cache.setPinned(conversationId, false) }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(processingIds = it.processingIds - conversationId) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    // ── Block ─────────────────────────────────────────────────────

    fun blockOther(conversation: Conversation) {
        val other = conversation.participants.firstOrNull { it.id != _state.value.currentUserId }
            ?: return
        _state.update { it.copy(actionTarget = null, processingIds = it.processingIds + conversation.id) }
        viewModelScope.launch {
            val r = blocking.block(other.id)
            _state.update { it.copy(processingIds = it.processingIds - conversation.id) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { s ->
                        s.copy(all = s.all.filterNot { it.id == conversation.id })
                    }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Report ────────────────────────────────────────────────────

    fun reportOther(conversation: Conversation, reason: ReportReason, description: String?) {
        val other = conversation.participants.firstOrNull { it.id != _state.value.currentUserId }
            ?: return
        viewModelScope.launch {
            val r = reporting.reportUser(other.id, reason, description)
            when (r) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    /**
     * يُستدعى عند العودة لشاشة القائمة. شاشة المحادثة تُغادر الغرفة عند إغلاقها،
     * لذا نعيد الانضمام لكل الغرف ثم نحدّث بصمت.
     */
    fun onScreenResumed() {
        joinedRooms.clear()
        joinConversationRooms(_state.value.all)
        refresh(silent = true)
    }

    /** ينضم لغرف كل المحادثات المقبولة حتى تصل أحداث الرسائل الجديدة على القائمة. */
    private fun joinConversationRooms(convs: List<Conversation>) {
        if (!_state.value.socketConnected) return
        convs.forEach { conv ->
            if (conv.status == "accepted" && joinedRooms.add(conv.id)) {
                socket.joinConversation(conv.id)
            }
        }
    }

    private suspend fun fetch(silent: Boolean) {
        when (val r = repo.fetchConversations()) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        all = r.data.conversations,
                        error = null
                    )
                }
                joinConversationRooms(r.data.conversations)
                runCatching { cache.saveConversations(r.data.conversations) }
            }
            is NetworkResult.Error -> {
                val msg = ErrorMessages.friendly(r)
                if (!silent && _state.value.all.isEmpty()) {
                    _state.update { it.copy(loading = false, refreshing = false, error = msg) }
                } else {
                    _state.update { it.copy(loading = false, refreshing = false) }
                    if (!silent) _message.tryEmit(msg)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return ChatsViewModel(
                    repo = app.conversationsRepository,
                    userRepo = app.userRepository,
                    socket = app.socket,
                    cache = app.chatsCacheStorage,
                    blocking = app.blockingRepository,
                    reporting = app.reportRepository
                ) as T
            }
        }
    }
}
