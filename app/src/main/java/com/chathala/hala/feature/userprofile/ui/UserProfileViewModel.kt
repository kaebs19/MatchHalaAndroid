package com.chathala.hala.feature.userprofile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.blocking.data.BlockingRepository
import com.chathala.hala.feature.discover.data.DiscoverRepository
import com.chathala.hala.feature.reporting.data.ReportReason
import com.chathala.hala.feature.reporting.data.ReportRepository
import com.chathala.hala.feature.user.data.UserRepository
import com.chathala.hala.feature.userprofile.data.UserProfile
import com.chathala.hala.feature.userprofile.data.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserProfileState(
    val loading: Boolean = true,
    val error: String? = null,
    val user: UserProfile? = null,
    val requesting: Boolean = false,
    val requestSent: Boolean = false,
    val liked: Boolean = false,
    val blocking: Boolean = false,
    val blocked: Boolean = false,
    val reporting: Boolean = false,
    val reported: Boolean = false,
    val currentUserPremium: Boolean = false
)

class UserProfileViewModel(
    private val userId: String,
    private val repo: UserProfileRepository,
    private val discover: DiscoverRepository,
    private val blocking: BlockingRepository,
    private val reporting: ReportRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    private val _openConversation = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openConversation: SharedFlow<String> = _openConversation.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            userRepo.currentUser.collect { u ->
                _state.update { it.copy(currentUserPremium = u?.isPremium == true) }
            }
        }
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.fetch(userId)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, user = r.data, error = null)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, error = ErrorMessages.friendly(r))
                }
            }
        }
    }

    /** إعجاب → يسجّل swipe فقط (لا يفتح/يُنشئ محادثة). عند تطابق متبادل: رسالة فقط. */
    fun likeUser() {
        if (_state.value.liked) return
        _state.update { it.copy(liked = true) }   // تغيّر اللون فوراً
        val name = _state.value.user?.name?.takeIf { it.isNotBlank() }
        _message.tryEmit(if (name != null) "تم الإعجاب بـ $name ❤️" else "تم الإعجاب ❤️")
        viewModelScope.launch {
            when (val r = discover.recordSwipe(userId, "like")) {
                is NetworkResult.Success ->
                    if (r.data.matched) _message.tryEmit(r.data.message ?: "تطابق جديد! 🎉")
                is NetworkResult.Error -> { /* «سبق السوايب» → تجاهل، نُبقي اللون */ }
            }
        }
    }

    /** سوبر لايك → يسجّل superlike (premium، حد يومي). */
    fun superLikeUser() {
        if (_state.value.liked) return
        _state.update { it.copy(liked = true) }
        viewModelScope.launch {
            when (val r = discover.recordSwipe(userId, "superlike")) {
                is NetworkResult.Success ->
                    if (r.data.matched) _message.tryEmit(r.data.message ?: "تطابق جديد! 🎉")
                is NetworkResult.Error -> {
                    _state.update { it.copy(liked = false) }  // تجاوز الحد → أعِد اللون
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun sendRequest(initialMessage: String? = null, isSuperLike: Boolean = false) {
        if (_state.value.requesting || _state.value.requestSent) return
        _state.update { it.copy(requesting = true) }
        viewModelScope.launch {
            when (val r = discover.requestConversation(userId, initialMessage, isSuperLike)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(requesting = false, requestSent = true) }
                    val msg = r.data.message
                        ?: if (r.data.isExisting) "محادثة موجودة — افتحها"
                        else "تم إرسال الطلب"
                    _message.tryEmit(msg)
                    val convId = r.data.conversationId
                    if (r.data.isExisting && convId != null) {
                        _openConversation.tryEmit(convId)
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(requesting = false) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun blockUser() {
        if (_state.value.blocking || _state.value.blocked) return
        _state.update { it.copy(blocking = true) }
        viewModelScope.launch {
            val r = blocking.block(userId)
            _state.update { it.copy(blocking = false) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(blocked = true) }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    fun unblockUser() {
        if (_state.value.blocking || !_state.value.blocked) return
        _state.update { it.copy(blocking = true) }
        viewModelScope.launch {
            val r = blocking.unblock(userId)
            _state.update { it.copy(blocking = false) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(blocked = false) }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    fun reportUser(reason: ReportReason, description: String?) {
        if (_state.value.reporting || _state.value.reported) return
        _state.update { it.copy(reporting = true) }
        viewModelScope.launch {
            val r = reporting.reportUser(userId, reason, description)
            _state.update { it.copy(reporting = false) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(reported = true) }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    companion object {
        fun factory(userId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return UserProfileViewModel(
                    userId = userId,
                    repo = app.userProfileRepository,
                    discover = app.discoverRepository,
                    blocking = app.blockingRepository,
                    reporting = app.reportRepository,
                    userRepo = app.userRepository
                ) as T
            }
        }
    }
}
