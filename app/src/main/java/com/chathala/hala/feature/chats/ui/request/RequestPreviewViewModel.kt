package com.chathala.hala.feature.chats.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.chats.data.ConversationsRepository
import com.chathala.hala.feature.chats.data.PendingRequest
import com.chathala.hala.feature.chats.socket.HalaSocket
import com.chathala.hala.feature.chats.socket.SocketEvent
import com.chathala.hala.feature.reporting.data.ReportReason
import com.chathala.hala.feature.reporting.data.ReportRepository
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

/**
 * شاشة معاينة طلب المحادثة بنمط iOS — تُفتح عند:
 *  - النقر على طلب من قائمة طلبات المحادثة
 *  - النقر على محادثة pending في قائمة المحادثات (إن لم تكن أنت المرسل)
 */
data class RequestPreviewUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val request: PendingRequest? = null,
    val processing: Boolean = false,
    val reporting: Boolean = false,
    val acceptedEvent: AcceptedEvent? = null,
    val rejected: Boolean = false
) {
    data class AcceptedEvent(val conversationId: String, val withWelcome: Boolean)
}

class RequestPreviewViewModel(
    private val conversationId: String,
    private val repo: ConversationsRepository,
    private val reportRepo: ReportRepository,
    private val socket: HalaSocket
) : ViewModel() {

    private val _state = MutableStateFlow(RequestPreviewUiState())
    val state: StateFlow<RequestPreviewUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        load()
        socket.incoming
            .onEach { evt ->
                // قد يلغي الطرف الآخر الطلب — نجدّد
                if (evt is SocketEvent.ConversationRequest) load()
            }
            .launchIn(viewModelScope)
    }

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            when (val r = repo.fetchPendingRequests()) {
                is NetworkResult.Success -> {
                    val req = r.data.conversations.firstOrNull { it.id == conversationId }
                    _state.update {
                        it.copy(
                            loading = false,
                            request = req,
                            notFound = req == null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(loading = false, notFound = true) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun accept(greeting: String? = null) {
        if (_state.value.processing) return
        _state.update { it.copy(processing = true) }
        viewModelScope.launch {
            val result = if (greeting.isNullOrBlank()) {
                repo.acceptRequest(conversationId)
            } else {
                repo.acceptRequestWithMessage(conversationId, greeting)
            }
            _state.update { it.copy(processing = false) }
            when (result) {
                is NetworkResult.Success -> {
                    repo.refreshPendingCount()
                    _state.update {
                        it.copy(
                            acceptedEvent = RequestPreviewUiState.AcceptedEvent(
                                conversationId = conversationId,
                                withWelcome = !greeting.isNullOrBlank()
                            )
                        )
                    }
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(result))
            }
        }
    }

    fun reject() {
        if (_state.value.processing) return
        _state.update { it.copy(processing = true) }
        viewModelScope.launch {
            val r = repo.rejectRequest(conversationId)
            _state.update { it.copy(processing = false) }
            when (r) {
                is NetworkResult.Success -> {
                    repo.refreshPendingCount()
                    _state.update { it.copy(rejected = true) }
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    fun report(reason: ReportReason, description: String?) {
        val uid = _state.value.request?.creator?.id ?: return
        if (_state.value.reporting) return
        _state.update { it.copy(reporting = true) }
        viewModelScope.launch {
            val r = reportRepo.reportUser(uid, reason, description)
            _state.update { it.copy(reporting = false) }
            when (r) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    companion object {
        fun factory(conversationId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                RequestPreviewViewModel(
                    conversationId = conversationId,
                    repo = app.conversationsRepository,
                    reportRepo = app.reportRepository,
                    socket = app.socket
                )
            }
        }
    }
}
