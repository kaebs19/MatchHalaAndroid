package com.chathala.hala.feature.settings.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.settings.data.DoNotDisturbData
import com.chathala.hala.feature.settings.data.NotificationPrefsData
import com.chathala.hala.feature.settings.data.SettingsRepository
import com.chathala.hala.feature.settings.data.UpdateNotificationPrefRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** مفاتيح تفضيلات الإشعارات القابلة للتبديل. */
enum class NotifPrefKey { PUSH_ENABLED, INVITATIONS, MESSAGES, PROFILE_VISITS, APP_ALERTS }

data class NotifSettingsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: NotificationPrefsData? = null,
    val dnd: DoNotDisturbData? = null,
    val updating: Boolean = false
)

class NotificationSettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotifSettingsUiState())
    val state: StateFlow<NotifSettingsUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.fetchNotificationPrefs()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, error = ErrorMessages.friendly(r))
                }
            }
            // ساعات الهدوء تأتي من إعدادات الخصوصية (privacySettings.doNotDisturb)
            when (val p = repo.fetchPrivacySettings()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(dnd = p.data.doNotDisturb ?: DoNotDisturbData())
                }
                is NetworkResult.Error -> { /* تجاهل — الإشعارات أهم؛ نُبقي dnd = null */ }
            }
        }
    }

    /** تفعيل/إيقاف ساعات الهدوء. */
    fun toggleQuietHours(enabled: Boolean) {
        val current = _state.value.dnd ?: DoNotDisturbData()
        _state.update { it.copy(updating = true, dnd = current.copy(enabled = enabled)) }
        viewModelScope.launch {
            when (val r = repo.setDoNotDisturb(enabled = enabled)) {
                is NetworkResult.Success -> _state.update { it.copy(dnd = r.data) }
                is NetworkResult.Error -> {
                    _state.update { it.copy(dnd = current) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _state.update { it.copy(updating = false) }
        }
    }

    /** تحديث وقت البداية أو النهاية لساعات الهدوء. */
    fun setQuietTime(isStart: Boolean, hour: Int, minute: Int) {
        val current = _state.value.dnd ?: DoNotDisturbData()
        val optimistic = if (isStart)
            current.copy(startHour = hour, startMinute = minute)
        else
            current.copy(endHour = hour, endMinute = minute)
        _state.update { it.copy(updating = true, dnd = optimistic) }
        viewModelScope.launch {
            val r = if (isStart)
                repo.setDoNotDisturb(enabled = optimistic.enabled, startHour = hour, startMinute = minute)
            else
                repo.setDoNotDisturb(enabled = optimistic.enabled, endHour = hour, endMinute = minute)
            when (r) {
                is NetworkResult.Success -> _state.update { it.copy(dnd = r.data) }
                is NetworkResult.Error -> {
                    _state.update { it.copy(dnd = current) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _state.update { it.copy(updating = false) }
        }
    }

    fun toggle(key: NotifPrefKey, value: Boolean) {
        val current = _state.value.data ?: return
        // تحديث متفائل
        _state.update { it.copy(updating = true, data = current.applied(key, value)) }
        viewModelScope.launch {
            val r = repo.updateNotificationPref(current.requestFor(key, value))
            when (r) {
                is NetworkResult.Success -> _state.update { it.copy(data = r.data) }
                is NetworkResult.Error -> {
                    // تراجع
                    _state.update { it.copy(data = current) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _state.update { it.copy(updating = false) }
        }
    }

    private fun NotificationPrefsData.applied(key: NotifPrefKey, value: Boolean) = when (key) {
        NotifPrefKey.PUSH_ENABLED -> copy(pushEnabled = value)
        NotifPrefKey.INVITATIONS -> copy(invitations = value)
        NotifPrefKey.MESSAGES -> copy(messages = value)
        NotifPrefKey.PROFILE_VISITS -> copy(profileVisits = value)
        NotifPrefKey.APP_ALERTS -> copy(appAlerts = value)
    }

    private fun NotificationPrefsData.requestFor(key: NotifPrefKey, value: Boolean) = when (key) {
        NotifPrefKey.PUSH_ENABLED -> UpdateNotificationPrefRequest(pushEnabled = value)
        NotifPrefKey.INVITATIONS -> UpdateNotificationPrefRequest(invitations = value)
        NotifPrefKey.MESSAGES -> UpdateNotificationPrefRequest(messages = value)
        NotifPrefKey.PROFILE_VISITS -> UpdateNotificationPrefRequest(profileVisits = value)
        NotifPrefKey.APP_ALERTS -> UpdateNotificationPrefRequest(appAlerts = value)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return NotificationSettingsViewModel(app.settingsRepository) as T
            }
        }
    }
}
