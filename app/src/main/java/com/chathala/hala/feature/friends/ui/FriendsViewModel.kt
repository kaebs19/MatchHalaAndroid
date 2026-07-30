package com.chathala.hala.feature.friends.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.friends.data.FriendItem
import com.chathala.hala.feature.friends.data.FriendRequestItem
import com.chathala.hala.feature.friends.data.FriendsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FriendsTab { FRIENDS, REQUESTS }

data class FriendsUiState(
    val tab: FriendsTab = FriendsTab.FRIENDS,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val friends: List<FriendItem> = emptyList(),
    val requests: List<FriendRequestItem> = emptyList(),
    val processingIds: Set<String> = emptySet()
)

class FriendsViewModel(
    private val repo: FriendsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val message: SharedFlow<String> = _message.asSharedFlow()

    private val _openConversation = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openConversation: SharedFlow<String> = _openConversation.asSharedFlow()

    init { load() }

    fun selectTab(tab: FriendsTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun load() {
        _state.update { it.copy(loading = it.friends.isEmpty() && it.requests.isEmpty()) }
        refreshBoth()
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        refreshBoth()
    }

    private fun refreshBoth() {
        viewModelScope.launch {
            when (val r = repo.list()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, error = null, friends = r.data.friends)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, refreshing = false, error = ErrorMessages.friendly(r))
                }
            }
        }
        viewModelScope.launch {
            when (val r = repo.requests()) {
                is NetworkResult.Success -> _state.update { it.copy(requests = r.data.requests) }
                is NetworkResult.Error -> { /* الطلبات ثانوية — تجاهل الخطأ */ }
            }
        }
    }

    fun accept(request: FriendRequestItem) {
        val id = request.friendshipId
        if (id in _state.value.processingIds) return
        _state.update { it.copy(processingIds = it.processingIds + id) }
        viewModelScope.launch {
            when (val r = repo.accept(id)) {
                is NetworkResult.Success -> {
                    _message.tryEmit("أصبحتما صديقين 🎉")
                    _state.update {
                        it.copy(
                            processingIds = it.processingIds - id,
                            requests = it.requests.filterNot { req -> req.friendshipId == id }
                        )
                    }
                    refreshBoth()
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(processingIds = it.processingIds - id) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun decline(request: FriendRequestItem) {
        val id = request.friendshipId
        if (id in _state.value.processingIds) return
        _state.update { it.copy(processingIds = it.processingIds + id) }
        viewModelScope.launch {
            when (val r = repo.decline(id)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        processingIds = it.processingIds - id,
                        requests = it.requests.filterNot { req -> req.friendshipId == id }
                    )
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(processingIds = it.processingIds - id) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun removeFriend(friend: FriendItem) {
        val uid = friend.user.id
        if (uid in _state.value.processingIds) return
        _state.update { it.copy(processingIds = it.processingIds + uid) }
        viewModelScope.launch {
            when (val r = repo.remove(uid)) {
                is NetworkResult.Success -> {
                    _message.tryEmit(r.data)
                    _state.update {
                        it.copy(
                            processingIds = it.processingIds - uid,
                            friends = it.friends.filterNot { f -> f.user.id == uid }
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(processingIds = it.processingIds - uid) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun openChat(friend: FriendItem) {
        friend.conversationId?.let { _openConversation.tryEmit(it) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return FriendsViewModel(repo = app.friendsRepository) as T
            }
        }
    }
}
