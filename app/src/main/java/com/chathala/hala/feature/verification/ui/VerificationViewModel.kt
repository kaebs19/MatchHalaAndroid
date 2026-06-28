package com.chathala.hala.feature.verification.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.user.data.UserRepository
import com.chathala.hala.feature.verification.data.VerificationRepository
import com.chathala.hala.feature.verification.data.VerificationStatusData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerificationUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val data: VerificationStatusData? = null,
    val selfieUri: Uri? = null
)

class VerificationViewModel(
    private val repo: VerificationRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.fetchStatus()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, error = ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun selectSelfie(uri: Uri?) {
        _state.update { it.copy(selfieUri = uri) }
    }

    fun submit(context: Context) {
        val uri = _state.value.selfieUri ?: return
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val r = repo.submit(context, uri)
            _state.update { it.copy(submitting = false) }
            when (r) {
                is NetworkResult.Success -> {
                    _message.tryEmit(r.data)
                    _state.update {
                        it.copy(
                            selfieUri = null,
                            data = (it.data ?: VerificationStatusData()).copy(status = "pending")
                        )
                    }
                    runCatching { userRepo.refresh() }
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
                return VerificationViewModel(
                    repo = app.verificationRepository,
                    userRepo = app.userRepository
                ) as T
            }
        }
    }
}
