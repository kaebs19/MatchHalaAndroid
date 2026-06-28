package com.chathala.hala.feature.discover.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.AppPreferences
import com.chathala.hala.feature.discover.data.DiscoverRepository
import com.chathala.hala.feature.discover.data.SearchUser
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** فلتر الجنس في البحث. */
enum class SearchGender(val api: String?) { ALL(null), MALE("male"), FEMALE("female") }

const val AGE_MIN = 18
const val AGE_MAX = 80

data class SearchFilters(
    val gender: SearchGender = SearchGender.ALL,
    val country: String? = null,        // ISO code أو null = الكل
    val minAge: Int = AGE_MIN,
    val maxAge: Int = AGE_MAX
) {
    val isActive: Boolean
        get() = gender != SearchGender.ALL || country != null || minAge != AGE_MIN || maxAge != AGE_MAX

    val minAgeParam: Int? get() = if (minAge > AGE_MIN) minAge else null
    val maxAgeParam: Int? get() = if (maxAge < AGE_MAX) maxAge else null
}

data class UserSearchUiState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val isPremium: Boolean = false,
    // بحث
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val results: List<SearchUser> = emptyList(),
    val page: Int = 1,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    // اقتراحات قبل الكتابة
    val suggestionsLoading: Boolean = false,
    val premium: List<SearchUser> = emptyList(),
    val online: List<SearchUser> = emptyList(),
    val onlineLoadingMore: Boolean = false,
    val onlineTotal: Int = 0,
    val recent: List<String> = emptyList()
) {
    val onlineCanLoadMore: Boolean get() = online.isNotEmpty() && online.size < onlineTotal
    val isSearchMode: Boolean get() = query.trim().length >= 2
}

class UserSearchViewModel(
    private val repo: DiscoverRepository,
    private val prefs: AppPreferences,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserSearchUiState())
    val state: StateFlow<UserSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private val pageSize = 20

    init {
        observeRecent()
        observePremium()
        loadSuggestions()
    }

    private fun observeRecent() {
        viewModelScope.launch {
            prefs.recentSearches.collect { list -> _state.update { it.copy(recent = list) } }
        }
    }

    private fun observePremium() {
        viewModelScope.launch {
            userRepo.currentUser.collect { u -> _state.update { it.copy(isPremium = u?.isPremium == true) } }
        }
    }

    fun loadSuggestions() {
        _state.update { it.copy(suggestionsLoading = true) }
        viewModelScope.launch {
            val f = _state.value.filters
            // random=true → مستخدمون مختلفون في كل دخول/تحديث
            // ✅ جلب القائمتين بالتوازي (أسرع استجابة)
            val premiumDeferred = async {
                repo.suggestedUsers(
                    isPremium = true, gender = f.gender.api, country = f.country,
                    minAge = f.minAgeParam, maxAge = f.maxAgeParam, random = true
                )
            }
            val onlineDeferred = async {
                repo.suggestedUsers(
                    online = true, gender = f.gender.api, country = f.country,
                    minAge = f.minAgeParam, maxAge = f.maxAgeParam, random = true
                )
            }
            val premiumRes = premiumDeferred.await()
            val onlineRes = onlineDeferred.await()
            val premium = (premiumRes as? NetworkResult.Success)?.data?.users ?: emptyList()
            val premiumIds = premium.map { it.id }.toSet()
            val onlineData = (onlineRes as? NetworkResult.Success)?.data
            val online = (onlineData?.users ?: emptyList()).filter { it.id !in premiumIds }
            _state.update {
                it.copy(
                    suggestionsLoading = false, premium = premium, online = online,
                    onlineTotal = onlineData?.total ?: online.size
                )
            }
        }
    }

    /** تحميل دفعة متصلين إضافية (عيّنة عشوائية مع إزالة التكرار). */
    fun loadMoreOnline() {
        val s = _state.value
        if (s.onlineLoadingMore || s.suggestionsLoading || s.isSearchMode || !s.onlineCanLoadMore) return
        _state.update { it.copy(onlineLoadingMore = true) }
        viewModelScope.launch {
            val f = s.filters
            val res = repo.suggestedUsers(
                online = true, gender = f.gender.api, country = f.country,
                minAge = f.minAgeParam, maxAge = f.maxAgeParam, random = true, limit = 12
            )
            val batch = (res as? NetworkResult.Success)?.data?.users ?: emptyList()
            val existing = (_state.value.premium.map { it.id } + _state.value.online.map { it.id }).toSet()
            val fresh = batch.filterNot { it.id in existing }
            _state.update {
                it.copy(
                    onlineLoadingMore = false,
                    online = it.online + fresh,
                    // لو لم تأتِ عناصر جديدة (نفدت العيّنة) أوقِف التحميل
                    onlineTotal = if (fresh.isEmpty()) it.online.size else it.onlineTotal
                )
            }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.trim().length < 2) {
            _state.update { it.copy(loading = false, results = emptyList(), error = null, searched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            runSearch(reset = true)
        }
    }

    /** يُطبَّق من ورقة الفلاتر (ميزة مدفوعة — التحقق في الواجهة). */
    fun applyFilters(filters: SearchFilters) {
        if (_state.value.filters == filters) return
        _state.update { it.copy(filters = filters) }
        if (_state.value.isSearchMode) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch { runSearch(reset = true) }
        } else {
            loadSuggestions()
        }
    }

    fun rememberQuery() {
        val q = _state.value.query.trim()
        if (q.length >= 2) viewModelScope.launch { prefs.addRecentSearch(q) }
    }

    fun applyRecent(term: String) = onQueryChange(term)
    fun removeRecent(term: String) { viewModelScope.launch { prefs.removeRecentSearch(term) } }
    fun clearRecent() { viewModelScope.launch { prefs.clearRecentSearches() } }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.canLoadMore || !s.isSearchMode) return
        searchJob = viewModelScope.launch { runSearch(reset = false) }
    }

    fun retry() {
        if (_state.value.isSearchMode) viewModelScope.launch { runSearch(reset = true) }
        else loadSuggestions()
    }

    private suspend fun runSearch(reset: Boolean) {
        val s = _state.value
        val f = s.filters
        val nextPage = if (reset) 1 else s.page + 1
        _state.update {
            if (reset) it.copy(loading = true, loadingMore = false, error = null)
            else it.copy(loadingMore = true)
        }
        val r = repo.searchUsers(
            s.query.trim(), page = nextPage,
            gender = f.gender.api, country = f.country,
            minAge = f.minAgeParam, maxAge = f.maxAgeParam
        )
        when (r) {
            is NetworkResult.Success -> {
                val incoming = r.data.users
                _state.update {
                    val merged = if (reset) incoming else it.results + incoming
                    it.copy(
                        loading = false, loadingMore = false, results = merged, page = nextPage,
                        canLoadMore = merged.size < r.data.total && incoming.size >= pageSize,
                        searched = true, error = null
                    )
                }
            }
            is NetworkResult.Error -> _state.update {
                it.copy(loading = false, loadingMore = false, searched = true, error = ErrorMessages.friendly(r))
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
                return UserSearchViewModel(app.discoverRepository, app.appPreferences, app.userRepository) as T
            }
        }
    }
}
