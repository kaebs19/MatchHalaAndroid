package com.chathala.hala.feature.settings.ui.content

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.AppPreferences
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.settings.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContentPreferencesViewModel(
    private val prefs: AppPreferences,
    private val repo: SettingsRepository
) : ViewModel() {

    val sensitiveContentEnabled: Flow<Boolean> = prefs.sensitiveContentEnabled

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setSensitiveContentEnabled(enabled)
            when (val result = repo.setAllowSensitiveContent(enabled)) {
                is NetworkResult.Error -> {
                    prefs.setSensitiveContentEnabled(!enabled)
                    _error.value = result.message
                }
                else -> {}
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ContentPreferencesViewModel(
                AppPreferences(context),
                SettingsRepository(tokenStorage = TokenStorage(context))
            ) as T
    }
}
