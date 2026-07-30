package com.chathala.hala.feature.visitors.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.visitors.data.ProfileViewItem
import com.chathala.hala.feature.visitors.data.VisitorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VisitorsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val totalViews: Int = 0,
    val views: List<ProfileViewItem> = emptyList(),
    /** true للمجاني — الأسماء/الصور مخفية ويجب الاشتراك لكشفها. */
    val premiumRequired: Boolean = false,
    val page: Int = 1,
    val totalPages: Int = 1,
    val loadingMore: Boolean = false
) {
    val canLoadMore: Boolean get() = !premiumRequired && page < totalPages
}

class VisitorsViewModel(private val repo: VisitorsRepository) : ViewModel() {

    private val _state = MutableStateFlow(VisitorsUiState())
    val state: StateFlow<VisitorsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = it.views.isEmpty(), error = null) }
        fetch(page = 1, append = false)
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        fetch(page = 1, append = false)
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.canLoadMore) return
        _state.update { it.copy(loadingMore = true) }
        fetch(page = s.page + 1, append = true)
    }

    private fun fetch(page: Int, append: Boolean) {
        viewModelScope.launch {
            when (val r = repo.fetchVisitors(page)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        error = null,
                        totalViews = r.data.totalViews,
                        views = if (append) it.views + r.data.views else r.data.views,
                        premiumRequired = r.data.isPremiumRequired,
                        page = r.data.page,
                        totalPages = r.data.totalPages
                    )
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        error = ErrorMessages.friendly(r)
                    )
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
                return VisitorsViewModel(app.visitorsRepository) as T
            }
        }
    }
}
