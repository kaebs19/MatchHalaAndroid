package com.chathala.hala.feature.discover.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.AppPreferences
import com.chathala.hala.core.util.ProfileFormatter
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
    /** نشطوا خلال الأسبوع الماضي وليسوا متصلين الآن — يُعرضون بعد قسم المتصلين. */
    val recentlyActive: List<SearchUser> = emptyList(),
    val recent: List<String> = emptyList(),
    /** true = شبكة، false = قائمة. محفوظ في التفضيلات فلا يُعاد ضبطه كل تشغيل. */
    val gridLayout: Boolean = true,
    /** تعديلات الإعجاب المحلية (تحديث متفائل) — تسبق ما جاء من الخادم. */
    val likedOverrides: Map<String, Boolean> = emptyMap(),
    /** طلبات إعجاب جارية — لمنع الضغط المتكرّر. */
    val likeInFlight: Set<String> = emptySet()
) {
    /** هل هذا المستخدم معجَب به الآن؟ */
    fun isLiked(user: SearchUser): Boolean =
        likedOverrides[user.id] ?: (user.isLiked == true)

    /**
     * `isNotEmpty` سقط من الشرط: كان يقفل «تحميل المزيد» نهائياً متى خرجت الدفعة
     * الأولى فارغة، فلا يتعافى القسم أبداً حتى لو كان الخادم يعرف مزيداً من المتصلين.
     */
    val onlineCanLoadMore: Boolean get() = online.size < onlineTotal
    val isSearchMode: Boolean get() = query.trim().length >= 2
}

class UserSearchViewModel(
    private val repo: DiscoverRepository,
    private val prefs: AppPreferences,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserSearchUiState())

    /** رسائل عابرة (فشل إعجاب مثلاً) — تُعرض كـ snackbar/toast في الشاشة. */
    private val _message = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: kotlinx.coroutines.flow.SharedFlow<String> = _message
    val state: StateFlow<UserSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private val pageSize = 20

    /** وقت آخر تحميل ناجح للاقتراحات — لكبح التحديث عند كل عودة للشاشة. */
    private var lastSuggestionsAt = 0L

    init {
        observeRecent()
        observePremium()
        observeLayout()
        loadSuggestions()
    }

    private fun observeLayout() {
        viewModelScope.launch {
            prefs.searchGridLayout.collect { grid -> _state.update { it.copy(gridLayout = grid) } }
        }
    }

    /**
     * إعجاب/إلغاء إعجاب من بطاقة البحث.
     *
     * تحديث متفائل: القلب يتلوّن فوراً قبل ردّ الخادم — الشبكة قد تتأخّر ثانية،
     * وقلبٌ لا يستجيب للمسة يبدو معطّلاً. عند الفشل نرجع للحالة السابقة.
     * `likeInFlight` يمنع الضغط المتكرّر السريع من إرسال طلبات متضاربة.
     */
    fun toggleLike(user: SearchUser) {
        val id = user.id
        if (id in _state.value.likeInFlight) return
        val nowLiked = !(likedState(id) ?: false)

        _state.update {
            it.copy(
                likedOverrides = it.likedOverrides + (id to nowLiked),
                likeInFlight = it.likeInFlight + id
            )
        }

        viewModelScope.launch {
            val r = if (nowLiked) repo.recordSwipe(id, "like") else repo.unlike(id)
            if (r is NetworkResult.Error) {
                _state.update {
                    it.copy(likedOverrides = it.likedOverrides + (id to !nowLiked))
                }
                _message.tryEmit(ErrorMessages.friendly(r))
            }
            _state.update { it.copy(likeInFlight = it.likeInFlight - id) }
        }
    }

    /** الحالة المعروضة: التعديل المحلي إن وُجد، وإلا ما جاء من الخادم. */
    private fun likedState(id: String): Boolean? =
        _state.value.likedOverrides[id]

    fun toggleLayout() {
        viewModelScope.launch { prefs.setSearchGridLayout(!_state.value.gridLayout) }
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
            // القوائم الثلاث بالتوازي (أسرع استجابة)
            val premiumDeferred = async {
                repo.suggestedUsers(
                    isPremium = true, gender = f.gender.api, country = f.country,
                    minAge = f.minAgeParam, maxAge = f.maxAgeParam, random = true
                )
            }
            val onlineDeferred = async {
                repo.suggestedUsers(
                    online = true, gender = f.gender.api, country = f.country,
                    minAge = f.minAgeParam, maxAge = f.maxAgeParam, random = true,
                    limit = ONLINE_PAGE
                )
            }
            val recentDeferred = async {
                repo.recentlyActiveUsers(
                    gender = f.gender.api, country = f.country,
                    minAge = f.minAgeParam, maxAge = f.maxAgeParam, limit = RECENT_PAGE
                )
            }
            val premiumRes = premiumDeferred.await()
            val onlineRes = onlineDeferred.await()
            val recentRes = recentDeferred.await()
            val premium = ((premiumRes as? NetworkResult.Success)?.data?.users ?: emptyList())
                .distinctBy { it.id }
            val onlineData = (onlineRes as? NetworkResult.Success)?.data
            // لا نحذف المشتركين من قائمة المتصلين: شريط «المشتركون» شريط ترشيح أفقي
            // منفصل، وكان استبعادهم يُفرغ قسم «متصلون الآن» كلّما كان أغلب المتصلين
            // مشتركين — وهو الحال الغالب في قاعدة مستخدمين صغيرة.
            // distinctBy ليس ترفاً: مفتاح مكرّر في LazyGrid يرمي استثناءً ويُسقط الشاشة،
            // والعيّنة العشوائية من الخادم قد تُعيد نفس المستخدم مرّتين.
            val online = (onlineData?.users ?: emptyList()).distinctBy { it.id }
            val recent = (recentRes as? NetworkResult.Success)?.data ?: emptyList()

            _state.update {
                it.copy(
                    suggestionsLoading = false,
                    premium = premium,
                    online = online,
                    onlineTotal = onlineData?.total ?: online.size,
                    recentlyActive = recent.asRecentlyActive(exclude = online.map { u -> u.id }.toSet())
                )
            }
            // الختم عند نجاح طلب واحد على الأقل: لو فشلت الثلاثة (انقطاع شبكة) لا نُجمّد
            // إعادة المحاولة دقيقتين — العودة للشاشة يجب أن تُعيد المحاولة فوراً.
            val anySucceeded = premiumRes is NetworkResult.Success ||
                onlineRes is NetworkResult.Success ||
                recentRes is NetworkResult.Success
            if (anySucceeded) lastSuggestionsAt = System.currentTimeMillis()
        }
    }

    /**
     * تحديث الاقتراحات عند العودة للشاشة — «متصل الآن» يفسد بمرور الوقت، وViewModel
     * يبقى حيّاً في مكدّس التنقّل فلا يُعاد `init`. مكبوح بـ[SUGGESTIONS_TTL_MS] حتى
     * لا يُطلق طلباً مع كل رجفة في دورة الحياة.
     */
    fun onScreenResumed() {
        val s = _state.value
        if (s.isSearchMode || s.suggestionsLoading) return
        if (System.currentTimeMillis() - lastSuggestionsAt < SUGGESTIONS_TTL_MS) return
        loadSuggestions()
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
            val existing = _state.value.online.map { it.id }.toSet()
            val fresh = batch.filterNot { it.id in existing }
            val freshIds = fresh.map { it.id }.toSet()
            _state.update {
                val merged = (it.online + fresh).distinctBy { u -> u.id }
                it.copy(
                    onlineLoadingMore = false,
                    online = merged,
                    // لو لم تأتِ عناصر جديدة (نفدت العيّنة) أوقِف التحميل
                    onlineTotal = if (fresh.isEmpty()) merged.size else it.onlineTotal,
                    // مَن ظهر الآن ضمن المتصلين لا يُكرَّر في «نشطون مؤخراً»
                    recentlyActive = it.recentlyActive.filterNot { u -> u.id in freshIds }
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
                    val merged = (if (reset) incoming else it.results + incoming).distinctBy { u -> u.id }
                    it.copy(
                        loading = false, loadingMore = false, results = merged.byPriority(),
                        page = nextPage,
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
        /** حجم دفعة «متصلون الآن» — 12 كانت تُخرج قسماً هزيلاً في شبكة من عمودين. */
        private const val ONLINE_PAGE = 24
        private const val RECENT_PAGE = 24

        /** عمر الاقتراحات قبل إعادة الجلب عند العودة للشاشة: دقيقتان. */
        private const val SUGGESTIONS_TTL_MS = 2 * 60 * 1000L

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

/**
 * أولوية عرض نتائج البحث: المميّزون أولاً، ثم المتصلون، ثم البقية.
 * sortedWith ثابت (stable) فيحافظ على ترتيب الخادم داخل كل مجموعة.
 */
private fun List<SearchUser>.byPriority(): List<SearchUser> = sortedWith(
    compareByDescending<SearchUser> { it.isPremium == true }
        .thenByDescending { it.isOnline == true }
)

/**
 * ترتيب «نشطون مؤخراً»: الأحدث ظهوراً أولاً، بلا مَن هو متصل الآن أو مكرّر.
 *
 * مَن لا يحمل `lastLogin` يبقى في القائمة (بعض إصدارات الخادم لا تُرسله) لكن في
 * آخرها — فلا يُزاحم نشاطاً معروف الوقت.
 */
private fun List<SearchUser>.asRecentlyActive(exclude: Set<String>): List<SearchUser> =
    asSequence()
        .filter { it.isOnline != true && it.id !in exclude }
        .distinctBy { it.id }
        .sortedBy { ProfileFormatter.millisSince(it.lastLogin) ?: Long.MAX_VALUE }
        .toList()
