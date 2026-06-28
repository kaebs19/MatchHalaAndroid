package com.chathala.hala.feature.discover.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.LatLng
import com.chathala.hala.core.util.LocationHelper
import com.chathala.hala.feature.blocking.data.BlockingRepository
import com.chathala.hala.feature.discover.data.DiscoverCacheStorage
import com.chathala.hala.feature.discover.data.DiscoverCard
import com.chathala.hala.feature.discover.data.DiscoverRepository
import com.chathala.hala.feature.reporting.data.ReportReason
import com.chathala.hala.feature.reporting.data.ReportRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val initialLoading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val cards: List<DiscoverCard> = emptyList(),
    val filters: DiscoverRepository.Filters = DiscoverRepository.Filters(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val requestingUserId: String? = null,
    val requestSentForIds: Set<String> = emptySet(),
    val blockingUserId: String? = null,
    val reportingUserId: String? = null,
    val reportedUserIds: Set<String> = emptySet(),
    val location: LatLng? = null,
    val currentIndex: Int = 0,
    val swipeHistory: List<Int> = emptyList(),
    val likedIds: Set<String> = emptySet()
) {
    val currentCard: DiscoverCard?
        get() = cards.getOrNull(currentIndex)

    val nextCard: DiscoverCard?
        get() = cards.getOrNull(currentIndex + 1)

    val canUndo: Boolean get() = swipeHistory.isNotEmpty()
}

class DiscoverViewModel(
    private val repo: DiscoverRepository,
    private val blocking: BlockingRepository,
    private val reporting: ReportRepository,
    private val cache: DiscoverCacheStorage,
    userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    val isPremium: StateFlow<Boolean> = userRepository.currentUser
        .map { it?.isPremium == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    private val _openConversation = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openConversation: SharedFlow<String> = _openConversation.asSharedFlow()

    // إشارة لعرض إعلان بيني بعد كل N بطاقة
    private val _showInterstitial = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitial: SharedFlow<Unit> = _showInterstitial.asSharedFlow()
    // إشارة لعرض إعلان مدمج بين البطاقات
    private val _showNativeAd = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showNativeAd: SharedFlow<Unit> = _showNativeAd.asSharedFlow()
    private var swipeCount = 0

    init { load() }

    fun load() {
        viewModelScope.launch {
            // 1) عرض الكاش فوراً إن وُجد (لا نُظهر شاشة التحميل)
            val cached = runCatching { cache.read() }.getOrNull()
            if (cached != null && cached.cards.isNotEmpty()) {
                _state.update {
                    it.copy(
                        initialLoading = false,
                        cards = cached.cards,
                        error = null,
                        currentIndex = 0,
                        swipeHistory = emptyList(),
                        refreshing = true
                    )
                }
            } else {
                _state.update { it.copy(initialLoading = true, error = null, page = 1) }
            }
            // 2) ثم نُحدّث من الشبكة بصمت
            fetch(page = 1, append = false)
        }
    }

    fun refresh() {
        if (_state.value.refreshing || _state.value.loadingMore) return
        _state.update { it.copy(refreshing = true, error = null, page = 1) }
        viewModelScope.launch { fetch(page = 1, append = false) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.refreshing || s.initialLoading) return
        if (s.page >= s.totalPages) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch { fetch(page = s.page + 1, append = true) }
    }

    fun applyFilters(filters: DiscoverRepository.Filters) {
        _state.update {
            it.copy(
                filters = filters,
                cards = emptyList(),
                initialLoading = true,
                error = null,
                page = 1
            )
        }
        viewModelScope.launch { fetch(page = 1, append = false) }
    }

    /** يحاول جلب الموقع ثم يعيد التحميل — يُستدعى بعد منح الإذن. */
    fun refreshLocationAndReload(context: Context) {
        viewModelScope.launch {
            val loc = LocationHelper.fetchLastLocation(context)
            if (loc != null) {
                _state.update { it.copy(location = loc, initialLoading = true, cards = emptyList(), page = 1) }
                fetch(page = 1, append = false)
            }
        }
    }

    private suspend fun fetch(page: Int, append: Boolean) {
        val f = _state.value.filters
        val loc = _state.value.location
        when (val r = repo.fetchCards(
            page = page,
            filters = f,
            latitude = loc?.lat,
            longitude = loc?.lng
        )) {
            is NetworkResult.Success -> {
                _state.update { s ->
                    // ✅ إزالة التكرار عند الدمج — الخادم قد يُعيد بطاقات لم يُمرَّر عليها بعد
                    // (خصوصاً المشتركين المتصدّرين بالنقاط)، فنستبعد ما هو موجود مسبقاً بالـ id.
                    val existingIds = s.cards.mapTo(HashSet()) { it.id }
                    val freshCards = r.data.cards.filterNot { it.id in existingIds }
                    val combined = if (append) s.cards + freshCards else r.data.cards.distinctBy { it.id }
                    // إذا لم تأتِ بطاقات جديدة عند الدمج → أوقف «تحميل المزيد» (تجنّب طلبات فارغة متكررة)
                    val effectiveTotalPages =
                        if (append && freshCards.isEmpty()) r.data.currentPage
                        else r.data.totalPages
                    s.copy(
                        initialLoading = false,
                        refreshing = false,
                        loadingMore = false,
                        cards = combined,
                        page = r.data.currentPage,
                        totalPages = effectiveTotalPages,
                        total = r.data.total,
                        error = null,
                        currentIndex = if (append) s.currentIndex else 0,
                        swipeHistory = if (append) s.swipeHistory else emptyList()
                    )
                }
                // حفظ الصفحة الأولى في الكاش فقط لتسريع الفتح القادم
                if (!append) {
                    runCatching { cache.save(r.data.cards) }
                }
            }
            is NetworkResult.Error -> {
                val msg = ErrorMessages.friendly(r)
                if (!append && _state.value.cards.isEmpty()) {
                    _state.update { it.copy(initialLoading = false, refreshing = false, loadingMore = false, error = msg) }
                } else {
                    _state.update { it.copy(initialLoading = false, refreshing = false, loadingMore = false) }
                    _message.tryEmit(msg)
                }
            }
        }
    }

    fun requestConversation(
        user: DiscoverCard,
        initialMessage: String? = null,
        isSuperLike: Boolean = false,
        // يُفتح الشات فقط من زر «رسالة» الصريح؛ الإعجاب/السوبر لايك لا يفتحان محادثة
        openIfExisting: Boolean = false,
        onFailure: (() -> Unit)? = null
    ) {
        val uid = user.id
        if (_state.value.requestingUserId == uid) return
        _state.update { it.copy(requestingUserId = uid) }
        viewModelScope.launch {
            when (val r = repo.requestConversation(uid, initialMessage, isSuperLike = isSuperLike)) {
                is NetworkResult.Success -> {
                    _state.update { s ->
                        s.copy(
                            requestingUserId = null,
                            requestSentForIds = s.requestSentForIds + uid
                        )
                    }
                    val msg = r.data.message ?: if (r.data.isExisting)
                        "محادثة موجودة — افتحها"
                    else
                        "تم إرسال الطلب"
                    _message.tryEmit(msg)
                    // إذا موجودة مسبقاً → افتحها فقط عند الطلب الصريح (زر «رسالة»)
                    val convId = r.data.conversationId
                    if (openIfExisting && r.data.isExisting && convId != null) {
                        _openConversation.tryEmit(convId)
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(requestingUserId = null) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                    onFailure?.invoke()
                }
            }
        }
    }

    /** يتخطى البطاقة الحالية (بدون طلب شبكة). */
    fun skipCurrent() {
        advance()
    }

    /**
     * Like → يسجّل إعجاباً فقط (لا يفتح/يُنشئ محادثة) + يلوّن الزر.
     * @param advance يتقدّم للبطاقة التالية (للسحب يميناً)؛ زر الإعجاب = false فيبقى المستخدم ظاهراً.
     * عند تطابق متبادل: رسالة «تطابق» فقط (بلا انتقال تلقائي).
     */
    fun likeCurrent(advance: Boolean = false) {
        val card = _state.value.currentCard ?: return
        val alreadyLiked = card.id in _state.value.likedIds
        if (!alreadyLiked) {
            _state.update { it.copy(likedIds = it.likedIds + card.id) }  // تغيّر اللون فوراً
            val name = card.name?.takeIf { it.isNotBlank() }
            _message.tryEmit(if (name != null) "تم الإعجاب بـ $name ❤️" else "تم الإعجاب ❤️")
            viewModelScope.launch {
                when (val r = repo.recordSwipe(card.id, "like")) {
                    is NetworkResult.Success ->
                        if (r.data.matched) _message.tryEmit(r.data.message ?: "تطابق جديد! 🎉")
                    is NetworkResult.Error -> { /* «سبق السوايب» وغيره → تجاهل بهدوء، نُبقي اللون */ }
                }
            }
        }
        if (advance) advance()
    }

    /** Super Like → يسجّل سوبر لايك (premium، له حد يومي)؛ يتقدّم. */
    fun superLikeCurrent(advance: Boolean = true) {
        val card = _state.value.currentCard ?: return
        if (card.id in _state.value.likedIds) { if (advance) advance(); return }
        _state.update { it.copy(likedIds = it.likedIds + card.id) }
        viewModelScope.launch {
            when (val r = repo.recordSwipe(card.id, "superlike")) {
                is NetworkResult.Success ->
                    if (r.data.matched) _message.tryEmit(r.data.message ?: "تطابق جديد! 🎉")
                is NetworkResult.Error -> {
                    // مثل تجاوز الحد اليومي → أظهر الرسالة وأعِد لون الزر
                    _state.update { it.copy(likedIds = it.likedIds - card.id) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
        if (advance) advance()
    }

    /** يرجع للبطاقة السابقة (بريميوم). */
    fun undoLast() {
        _state.update { s ->
            val prev = s.swipeHistory.lastOrNull() ?: return@update s
            s.copy(
                currentIndex = prev,
                swipeHistory = s.swipeHistory.dropLast(1)
            )
        }
    }

    private fun advance() {
        _state.update { s ->
            s.copy(
                currentIndex = s.currentIndex + 1,
                swipeHistory = s.swipeHistory + s.currentIndex
            )
        }
        // إعلانات الاكتشاف: مدمج بعد كل N بطاقة، بيني بعد كل M بطاقة
        swipeCount++
        if (swipeCount % com.chathala.hala.core.ads.AdConfig.NATIVE_EVERY_CARDS == 0) {
            _showNativeAd.tryEmit(Unit)
        }
        if (swipeCount % com.chathala.hala.core.ads.AdConfig.DISCOVER_INTERSTITIAL_EVERY_CARDS == 0) {
            _showInterstitial.tryEmit(Unit)
        }
        val s = _state.value
        val remaining = s.cards.size - s.currentIndex
        if (remaining <= 4 && s.page < s.totalPages && !s.loadingMore) {
            loadMore()
        }
    }

    fun blockUser(user: DiscoverCard) {
        if (_state.value.blockingUserId == user.id) return
        _state.update { it.copy(blockingUserId = user.id) }
        viewModelScope.launch {
            val r = blocking.block(user.id)
            _state.update { it.copy(blockingUserId = null) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { s ->
                        s.copy(cards = s.cards.filterNot { it.id == user.id })
                    }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    fun reportUser(user: DiscoverCard, reason: ReportReason, description: String?) {
        if (_state.value.reportingUserId == user.id) return
        _state.update { it.copy(reportingUserId = user.id) }
        viewModelScope.launch {
            val r = reporting.reportUser(user.id, reason, description)
            _state.update { it.copy(reportingUserId = null) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { s ->
                        s.copy(reportedUserIds = s.reportedUserIds + user.id)
                    }
                    _message.tryEmit(r.data)
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
                return DiscoverViewModel(
                    repo = app.discoverRepository,
                    blocking = app.blockingRepository,
                    reporting = app.reportRepository,
                    cache = app.discoverCacheStorage,
                    userRepository = app.userRepository
                ) as T
            }
        }
    }
}
