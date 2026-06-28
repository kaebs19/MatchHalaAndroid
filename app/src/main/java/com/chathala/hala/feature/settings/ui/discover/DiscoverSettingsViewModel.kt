package com.chathala.hala.feature.settings.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.settings.data.DiscoveryPausedData
import com.chathala.hala.feature.settings.data.PrivacySettingsData
import com.chathala.hala.feature.settings.data.SettingsRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** الإعدادات القابلة للتبديل في شاشة الاكتشاف. */
enum class DiscoverPref { STEALTH, SHOW_AGE, SHOW_COUNTRY, ACCEPTING_REQUESTS, PREMIUM_ONLY_REQUESTS }

data class DiscoverSettingsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: PrivacySettingsData? = null,
    val updating: Boolean = false,
    val isPremium: Boolean = false
)

class DiscoverSettingsViewModel(
    private val repo: SettingsRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverSettingsUiState())
    val state: StateFlow<DiscoverSettingsUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        observePremium()
        load()
    }

    private fun observePremium() {
        viewModelScope.launch {
            userRepo.currentUser.collect { u ->
                _state.update { it.copy(isPremium = u?.isPremium == true) }
            }
        }
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.fetchPrivacySettings()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, error = ErrorMessages.friendly(r))
                }
            }
        }
    }

    /**
     * @param pref الإعداد
     * @param serverValue القيمة المُرسلة للخادم (القيمة الفعلية للحقل، لا قيمة المفتاح المعروض)
     */
    fun toggle(pref: DiscoverPref, serverValue: Boolean) {
        val current = _state.value.data ?: return

        // بوابة المشتركين — التفعيل فقط يتطلب اشتراكاً
        val premiumGated = pref == DiscoverPref.STEALTH || pref == DiscoverPref.PREMIUM_ONLY_REQUESTS
        if (premiumGated && serverValue && !_state.value.isPremium) {
            _message.tryEmit("هذه الميزة للمشتركين فقط 👑")
            return
        }

        // تحديث متفائل
        _state.update { it.copy(updating = true, data = current.applied(pref, serverValue)) }
        viewModelScope.launch {
            val r = when (pref) {
                DiscoverPref.STEALTH -> repo.setStealthMode(serverValue)
                DiscoverPref.SHOW_AGE -> repo.setShowAge(serverValue)
                DiscoverPref.SHOW_COUNTRY -> repo.setShowCountry(serverValue)
                DiscoverPref.ACCEPTING_REQUESTS -> repo.setAcceptingRequests(serverValue)
                DiscoverPref.PREMIUM_ONLY_REQUESTS -> repo.setPremiumOnlyRequests(serverValue)
            }
            when (r) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> {
                    _state.update { it.copy(data = current) } // تراجع
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _state.update { it.copy(updating = false) }
        }
    }

    /**
     * إيقاف/استئناف الظهور في الاكتشاف (للمشتركين عند التفعيل).
     * @param enabled true = إيقاف الظهور
     * @param durationHours المدة بالساعات، null = حتى الاستئناف اليدوي
     */
    fun setPauseDiscovery(enabled: Boolean, durationHours: Int? = null) {
        val current = _state.value.data ?: return

        if (enabled && !_state.value.isPremium) {
            _message.tryEmit("هذه الميزة للمشتركين فقط 👑")
            return
        }

        // تحديث متفائل — الوقت الفعلي until يأتي من الخادم عند النجاح (نُعيد التحميل)
        _state.update {
            it.copy(updating = true, data = current.copy(discoveryPaused = DiscoveryPausedData(enabled = enabled)))
        }
        viewModelScope.launch {
            when (val r = repo.setPauseDiscovery(enabled, durationHours)) {
                is NetworkResult.Success -> {
                    _message.tryEmit(r.data)
                    // إعادة جلب لمعرفة until الدقيق
                    refreshSilently()
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(data = current) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _state.update { it.copy(updating = false) }
        }
    }

    private fun refreshSilently() {
        viewModelScope.launch {
            when (val r = repo.fetchPrivacySettings()) {
                is NetworkResult.Success -> _state.update { it.copy(data = r.data) }
                is NetworkResult.Error -> { /* تجاهل */ }
            }
        }
    }

    private fun PrivacySettingsData.applied(pref: DiscoverPref, v: Boolean) = when (pref) {
        DiscoverPref.STEALTH -> copy(stealthMode = v)
        DiscoverPref.SHOW_AGE -> copy(showAge = v)
        DiscoverPref.SHOW_COUNTRY -> copy(showCountry = v)
        DiscoverPref.ACCEPTING_REQUESTS -> copy(acceptingRequests = v)
        DiscoverPref.PREMIUM_ONLY_REQUESTS -> copy(premiumOnlyRequests = v)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return DiscoverSettingsViewModel(app.settingsRepository, app.userRepository) as T
            }
        }
    }
}
