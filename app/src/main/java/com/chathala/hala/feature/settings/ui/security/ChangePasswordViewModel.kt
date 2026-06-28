package com.chathala.hala.feature.settings.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.Validators
import com.chathala.hala.feature.settings.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val currentError: String? = null,
    val newError: String? = null,
    val confirmError: String? = null
)

class ChangePasswordViewModel(private val repo: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    fun setCurrent(v: String) = _state.update { it.copy(current = v, currentError = null) }
    fun setNew(v: String) = _state.update { it.copy(new = v, newError = null) }
    fun setConfirm(v: String) = _state.update { it.copy(confirm = v, confirmError = null) }

    fun submit() {
        val s = _state.value
        val currentErr = if (s.current.isBlank()) "كلمة المرور الحالية مطلوبة" else null
        val newErr = Validators.password(s.new)
        val confirmErr = Validators.passwordConfirm(s.new, s.confirm)
        if (currentErr != null || newErr != null || confirmErr != null) {
            _state.update {
                it.copy(
                    currentError = currentErr,
                    newError = newErr,
                    confirmError = confirmErr
                )
            }
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.changePassword(s.current, s.new)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, success = true)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, error = ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun clearSuccess() = _state.update { it.copy(success = false) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return ChangePasswordViewModel(app.settingsRepository) as T
            }
        }
    }
}
