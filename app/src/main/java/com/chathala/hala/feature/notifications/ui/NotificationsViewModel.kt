package com.chathala.hala.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.notifications.data.NotificationItem
import com.chathala.hala.feature.notifications.data.NotificationsRepository
import com.chathala.hala.feature.notifications.data.ReadByEntry
import com.chathala.hala.feature.user.data.User
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class NotificationFilter(val apiValue: String) {
    ALL("all"),
    UNREAD("unread"),
    SOCIAL("social"),
    SYSTEM("system")
}

data class NotificationsUiState(
    val initialLoading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val items: List<NotificationItem> = emptyList(),
    val filter: NotificationFilter = NotificationFilter.ALL,
    val unreadCount: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val currentUserId: String? = null,
    val bulkWorking: Boolean = false
)

class NotificationsViewModel(
    private val repo: NotificationsRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        viewModelScope.launch {
            val u: User? = userRepo.currentUser.first()
            _state.update { it.copy(currentUserId = u?.id) }
            fetch(page = 1, append = false)
        }
    }

    fun refresh() {
        val s = _state.value
        if (s.refreshing || s.loadingMore) return
        _state.update { it.copy(refreshing = true, error = null, page = 1) }
        viewModelScope.launch { fetch(page = 1, append = false) }
    }

    fun retry() {
        _state.update { it.copy(initialLoading = true, error = null, page = 1) }
        viewModelScope.launch { fetch(page = 1, append = false) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.refreshing || s.initialLoading) return
        if (s.page >= s.totalPages) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch { fetch(page = s.page + 1, append = true) }
    }

    fun selectFilter(filter: NotificationFilter) {
        if (filter == _state.value.filter) return
        _state.update {
            it.copy(
                filter = filter,
                items = emptyList(),
                initialLoading = true,
                error = null,
                page = 1
            )
        }
        viewModelScope.launch { fetch(page = 1, append = false) }
    }

    private suspend fun fetch(page: Int, append: Boolean) {
        val filter = _state.value.filter.apiValue
        when (val r = repo.fetchNotifications(page = page, filter = filter)) {
            is NetworkResult.Success -> {
                repo.updateUnreadCount(r.data.unreadCount)
                _state.update { cur ->
                    val combined = if (append) cur.items + r.data.notifications else r.data.notifications
                    cur.copy(
                        initialLoading = false,
                        refreshing = false,
                        loadingMore = false,
                        items = combined,
                        unreadCount = r.data.unreadCount,
                        page = r.data.currentPage,
                        totalPages = r.data.totalPages,
                        error = null
                    )
                }
            }
            is NetworkResult.Error -> {
                val friendly = ErrorMessages.friendly(r)
                if (!append && _state.value.items.isEmpty()) {
                    _state.update {
                        it.copy(
                            initialLoading = false,
                            refreshing = false,
                            loadingMore = false,
                            error = friendly
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            initialLoading = false,
                            refreshing = false,
                            loadingMore = false
                        )
                    }
                    _message.tryEmit(friendly)
                }
            }
        }
    }

    /** يدوياً — عند الضغط على إشعار غير مقروء (backend أصلاً auto-read عند fetch). */
    fun markRead(id: String) {
        val uid = _state.value.currentUserId ?: return
        val target = _state.value.items.firstOrNull { it.id == id } ?: return
        if (target.readBy?.any { it.user == uid } == true) return

        // Optimistic: أضف readBy محلياً وأنقص العدّاد
        val updated = target.copy(
            readBy = (target.readBy ?: emptyList()) +
                ReadByEntry(user = uid, readAt = nowIsoUtc())
        )
        _state.update { cur ->
            cur.copy(
                items = cur.items.map { if (it.id == id) updated else it },
                unreadCount = (cur.unreadCount - 1).coerceAtLeast(0)
            )
        }
        repo.decrementUnread(1)

        viewModelScope.launch {
            val r = repo.markRead(id)
            if (r is NetworkResult.Error) {
                _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    fun markAllRead() {
        if (_state.value.bulkWorking || _state.value.unreadCount == 0) return
        _state.update { it.copy(bulkWorking = true) }
        viewModelScope.launch {
            when (val r = repo.markAllRead()) {
                is NetworkResult.Success -> {
                    _message.tryEmit(r.data)
                    repo.clearUnread()
                    _state.update { it.copy(unreadCount = 0) }
                    fetch(page = 1, append = false)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
            _state.update { it.copy(bulkWorking = false) }
        }
    }

    fun delete(id: String) {
        val previous = _state.value.items
        val target = previous.firstOrNull { it.id == id }
        val wasUnread = target != null && _state.value.currentUserId?.let { uid ->
            target.readBy?.any { it.user == uid } != true
        } == true

        _state.update { cur ->
            cur.copy(
                items = previous.filterNot { it.id == id },
                unreadCount = if (wasUnread) (cur.unreadCount - 1).coerceAtLeast(0) else cur.unreadCount
            )
        }
        if (wasUnread) repo.decrementUnread(1)

        viewModelScope.launch {
            val r = repo.delete(id)
            if (r is NetworkResult.Error) {
                _state.update { it.copy(items = previous) }
                if (wasUnread) repo.updateUnreadCount(_state.value.unreadCount + 1)
                _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    fun deleteAll() {
        if (_state.value.bulkWorking || _state.value.items.isEmpty()) return
        _state.update { it.copy(bulkWorking = true) }
        viewModelScope.launch {
            when (val r = repo.deleteAll()) {
                is NetworkResult.Success -> {
                    _message.tryEmit(r.data)
                    repo.clearUnread()
                    _state.update { it.copy(items = emptyList(), unreadCount = 0) }
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
            _state.update { it.copy(bulkWorking = false) }
        }
    }

    private fun nowIsoUtc(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return NotificationsViewModel(
                    repo = app.notificationsRepository,
                    userRepo = app.userRepository
                ) as T
            }
        }
    }
}
