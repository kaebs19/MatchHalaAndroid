package com.chathala.hala.feature.settings.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.settings.data.PrivacySettingsData
import com.chathala.hala.feature.settings.data.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: PrivacySettingsData? = null,
    val updating: Boolean = false
)

class PrivacyViewModel(private val repo: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.fetchPrivacySettings()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, data = r.data)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, error = ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun toggleDistance(value: Boolean) {
        val current = _state.value.data ?: return
        _state.update { it.copy(updating = true, data = current.copy(showDistance = value)) }
        viewModelScope.launch {
            when (val r = repo.setShowDistance(value)) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> {
                    // revert
                    _state.update { it.copy(data = current.copy(showDistance = !value)) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _state.update { it.copy(updating = false) }
        }
    }

    fun toggleStealth(value: Boolean) {
        val current = _state.value.data ?: return
        _state.update { it.copy(updating = true, data = current.copy(stealthMode = value)) }
        viewModelScope.launch {
            when (val r = repo.setStealthMode(value)) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> {
                    _state.update { it.copy(data = current.copy(stealthMode = !value)) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _state.update { it.copy(updating = false) }
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
                return PrivacyViewModel(app.settingsRepository) as T
            }
        }
    }
}
