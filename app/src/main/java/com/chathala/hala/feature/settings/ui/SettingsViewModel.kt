package com.chathala.hala.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.AppPreferences
import com.chathala.hala.core.storage.AppTheme
import com.chathala.hala.feature.auth.data.AuthRepository
import com.chathala.hala.feature.user.data.User
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: AppPreferences,
    private val authRepo: AuthRepository,
    userRepo: UserRepository
) : ViewModel() {

    val theme: Flow<AppTheme> = prefs.theme

    val currentUser: StateFlow<User?> = userRepo.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }

    fun deleteAccount(password: String?, onDone: () -> Unit) {
        if (_deleting.value) return
        _deleting.value = true
        viewModelScope.launch {
            when (val r = authRepo.deleteAccount(password)) {
                is NetworkResult.Success -> {
                    _message.tryEmit(r.data)
                    onDone()
                }
                is NetworkResult.Error -> {
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
            _deleting.value = false
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
                return SettingsViewModel(
                    prefs = app.appPreferences,
                    authRepo = app.authRepository,
                    userRepo = app.userRepository
                ) as T
            }
        }
    }
}
