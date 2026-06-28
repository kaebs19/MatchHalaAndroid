package com.chathala.hala.feature.blocking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.blocking.data.BlockedUser
import com.chathala.hala.feature.blocking.data.BlockingRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val users: List<BlockedUser> = emptyList(),
    val processingIds: Set<String> = emptySet()
)

class BlockedUsersViewModel(
    private val repo: BlockingRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedUsersUiState())
    val state: StateFlow<BlockedUsersUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { fetch() }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        when (val r = repo.fetchBlocked()) {
            is NetworkResult.Success -> _state.update {
                it.copy(loading = false, refreshing = false, users = r.data, error = null)
            }
            is NetworkResult.Error -> _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    error = if (it.users.isEmpty()) ErrorMessages.friendly(r) else null
                )
            }
        }
    }

    fun unblock(user: BlockedUser) {
        if (user.id in _state.value.processingIds) return
        _state.update { it.copy(processingIds = it.processingIds + user.id) }
        viewModelScope.launch {
            val r = repo.unblock(user.id)
            _state.update { it.copy(processingIds = it.processingIds - user.id) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { s -> s.copy(users = s.users.filterNot { it.id == user.id }) }
                    _message.tryEmit(r.data)
                    runCatching { userRepo.refresh() }
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
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
                return BlockedUsersViewModel(
                    repo = app.blockingRepository,
                    userRepo = app.userRepository
                ) as T
            }
        }
    }
}
