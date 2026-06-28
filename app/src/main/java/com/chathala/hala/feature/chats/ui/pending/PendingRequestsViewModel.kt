package com.chathala.hala.feature.chats.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.chats.data.ConversationsRepository
import com.chathala.hala.feature.chats.data.InitialMessage
import com.chathala.hala.feature.chats.data.PendingRequest
import com.chathala.hala.feature.chats.data.PendingRequestCreator
import com.chathala.hala.feature.chats.socket.HalaSocket
import com.chathala.hala.feature.chats.socket.SocketEvent
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PendingTab { RECEIVED, SENT }

data class PendingUiState(
    val tab: PendingTab = PendingTab.RECEIVED,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val received: List<PendingRequest> = emptyList(),
    val sent: List<PendingRequest> = emptyList(),
    val processingIds: Set<String> = emptySet()
) {
    val items: List<PendingRequest>
        get() = if (tab == PendingTab.RECEIVED) received else sent
    val receivedCount: Int get() = received.size
    val sentCount: Int get() = sent.size
}

class PendingRequestsViewModel(
    private val repo: ConversationsRepository,
    private val userRepo: UserRepository,
    private val socket: HalaSocket
) : ViewModel() {

    private val _state = MutableStateFlow(PendingUiState())
    val state: StateFlow<PendingUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    private val _acceptedEvent = MutableSharedFlow<AcceptedEvent>(extraBufferCapacity = 1)
    val acceptedEvent: SharedFlow<AcceptedEvent> = _acceptedEvent.asSharedFlow()

    init {
        load()
        socket.incoming
            .onEach { evt ->
                if (evt is SocketEvent.ConversationRequest ||
                    evt is SocketEvent.ConversationAccepted ||
                    evt is SocketEvent.ConversationRejected
                ) refresh(silent = true)
            }
            .launchIn(viewModelScope)
    }

    fun selectTab(tab: PendingTab) {
        if (_state.value.tab == tab) return
        _state.update { it.copy(tab = tab) }
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { fetchBoth(silent = false) }
    }

    fun refresh(silent: Boolean = false) {
        if (_state.value.refreshing) return
        if (!silent) _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch { fetchBoth(silent = silent) }
    }

    private suspend fun fetchBoth(silent: Boolean) {
        val selfId = userRepo.currentUser.first()?.id
        // 1) المستلَمة من endpoint pending
        val receivedResult = repo.fetchPendingRequests()
        // 2) المرسَلة — نشتق من قائمة المحادثات (status=pending && creator=self)
        val sentResult = repo.fetchConversations()

        when (receivedResult) {
            is NetworkResult.Success -> {
                _state.update { it.copy(received = receivedResult.data.conversations) }
            }
            is NetworkResult.Error -> {
                val msg = ErrorMessages.friendly(receivedResult)
                if (!silent && _state.value.received.isEmpty() && _state.value.sent.isEmpty()) {
                    _state.update { it.copy(loading = false, refreshing = false, error = msg) }
                    return
                } else if (!silent) {
                    _message.tryEmit(msg)
                }
            }
        }

        if (sentResult is NetworkResult.Success && selfId != null) {
            val sentPending = sentResult.data.conversations
                .filter { it.status == "pending" && it.creator == selfId }
                .map { conv ->
                    val other = conv.participants.firstOrNull { it.id != selfId }
                    PendingRequest(
                        id = conv.id,
                        status = conv.status,
                        chatMode = conv.chatMode,
                        isSuperLike = false,
                        creator = other?.let {
                            PendingRequestCreator(
                                id = it.id,
                                name = it.name,
                                profileImage = it.profileImage,
                                isPremium = it.isPremium,
                                isVerified = it.verification?.isVerified
                            )
                        },
                        initialMessage = conv.initialMessage?.let {
                            InitialMessage(content = it.content, createdAt = it.createdAt)
                        },
                        createdAt = conv.createdAt
                    )
                }
            _state.update { it.copy(sent = sentPending) }
        }

        _state.update { it.copy(loading = false, refreshing = false) }
    }

    fun accept(id: String, greeting: String? = null) {
        if (id in _state.value.processingIds) return
        _state.update { it.copy(processingIds = it.processingIds + id) }
        viewModelScope.launch {
            val result = if (greeting.isNullOrBlank()) {
                when (val r = repo.acceptRequest(id)) {
                    is NetworkResult.Success -> Result.success(AcceptedEvent(id, welcomeSent = false))
                    is NetworkResult.Error -> Result.failure(Exception(ErrorMessages.friendly(r)))
                }
            } else {
                when (val r = repo.acceptRequestWithMessage(id, greeting)) {
                    is NetworkResult.Success -> Result.success(AcceptedEvent(id, welcomeSent = true))
                    is NetworkResult.Error -> Result.failure(Exception(ErrorMessages.friendly(r)))
                }
            }

            _state.update { it.copy(processingIds = it.processingIds - id) }
            result.fold(
                onSuccess = { evt ->
                    _state.update { s ->
                        s.copy(received = s.received.filterNot { it.id == id })
                    }
                    repo.refreshPendingCount()
                    _acceptedEvent.tryEmit(evt)
                    _message.tryEmit(
                        if (evt.welcomeSent) "تم القبول وإرسال الترحيب" else "تم قبول الطلب"
                    )
                },
                onFailure = { e -> _message.tryEmit(e.message ?: "فشل قبول الطلب") }
            )
        }
    }

    fun reject(id: String) {
        if (id in _state.value.processingIds) return
        _state.update { it.copy(processingIds = it.processingIds + id) }
        viewModelScope.launch {
            val r = repo.rejectRequest(id)
            _state.update { it.copy(processingIds = it.processingIds - id) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { s ->
                        s.copy(received = s.received.filterNot { it.id == id })
                    }
                    repo.refreshPendingCount()
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    /** للمرسَلة: إلغاء طلب أنشأته أنت (نستخدم نفس endpoint رفض). */
    fun cancelSent(id: String) {
        if (id in _state.value.processingIds) return
        _state.update { it.copy(processingIds = it.processingIds + id) }
        viewModelScope.launch {
            val r = repo.deleteConversation(id)
            _state.update { it.copy(processingIds = it.processingIds - id) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { s ->
                        s.copy(sent = s.sent.filterNot { it.id == id })
                    }
                    _message.tryEmit("تم إلغاء الطلب")
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    data class AcceptedEvent(val conversationId: String, val welcomeSent: Boolean)

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return PendingRequestsViewModel(
                    repo = app.conversationsRepository,
                    userRepo = app.userRepository,
                    socket = app.socket
                ) as T
            }
        }
    }
}
