package com.chathala.hala.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.auth.data.AuthRepository
import com.chathala.hala.feature.auth.data.ErrorBody
import com.chathala.hala.feature.suspension.data.AccountSuspensionInfo
import com.chathala.hala.feature.suspension.data.SuspensionGate
import com.chathala.hala.feature.suspension.data.SuspensionMode
import com.chathala.hala.feature.chats.socket.HalaSocket
import com.chathala.hala.feature.push.data.DeviceTokenRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val done: Boolean = false,
    /** عند تعيينه → يجب التوجيه لشاشة الحظر بالوضع المحدّد (إيقاف حساب / حظر جهاز). */
    val bannedMode: SuspensionMode? = null
)

class AuthViewModel(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val deviceTokenRepo: DeviceTokenRepository,
    private val socket: HalaSocket
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun resetFeedback() {
        _state.update { it.copy(error = null, successMessage = null, done = false, bannedMode = null) }
    }

    fun login(email: String, password: String) {
        runAuth { authRepo.login(email, password) }
    }

    fun register(name: String, email: String, password: String) {
        runAuth { authRepo.register(name, email, password) }
    }

    fun googleLogin(idToken: String) {
        runAuth { authRepo.googleLogin(idToken) }
    }

    /**
     * Wrapper موحّد لتدفق تسجيل الدخول: يستدعي الـ API، وعند النجاح
     * يحاول جلب بيانات المستخدم الكاملة من /me (best-effort — لا يمنع المتابعة لو فشل).
     */
    private fun runAuth(block: suspend () -> NetworkResult<*>) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = block()) {
                is NetworkResult.Success -> {
                    userRepo.refresh()   // ← يحدّث بيانات المستخدم من /api/auth/me
                    // سجّل FCM token بـ best-effort — لا يمنع إتمام الدخول إن فشل
                    runCatching { deviceTokenRepo.ensureSynced() }
                    // افتح اتصال Socket.IO
                    socket.connect()
                    _state.update { it.copy(loading = false, done = true) }
                }
                is NetworkResult.Error -> handleAuthError(r)
            }
        }
    }

    /** يفرّق بين الإيقاف/الحظر (توجيه لشاشة مخصّصة) وبقية الأخطاء (رسالة عادية). */
    private fun handleAuthError(error: NetworkResult.Error) {
        when (error.code) {
            "ACCOUNT_SUSPENDED" -> {
                val body = error.payload as? ErrorBody
                val data = body?.data
                val until = data?.suspendedUntil
                SuspensionGate.account = AccountSuspensionInfo(
                    name = body?.user?.name,
                    email = body?.user?.email,
                    userId = body?.user?.id,
                    reason = data?.reason,
                    suspendedUntil = until,
                    level = data?.level ?: 0,
                    isPermanent = until.isNullOrBlank(),
                    token = body?.token
                )
                _state.update { it.copy(loading = false, error = null, bannedMode = SuspensionMode.ACCOUNT) }
            }
            "DEVICE_BANNED" -> {
                SuspensionGate.account = null
                _state.update { it.copy(loading = false, error = null, bannedMode = SuspensionMode.DEVICE) }
            }
            else ->
                _state.update { it.copy(loading = false, error = ErrorMessages.friendly(error)) }
        }
    }

    fun setError(message: String) {
        _state.update { it.copy(loading = false, error = message) }
    }

    fun forgotPassword(email: String) {
        _state.update { it.copy(loading = true, error = null, successMessage = null) }
        viewModelScope.launch {
            when (val r = authRepo.forgotPassword(email)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, successMessage = r.data, done = true)
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(loading = false, error = ErrorMessages.friendly(r)) }
            }
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String) {
        _state.update { it.copy(loading = true, error = null, successMessage = null) }
        viewModelScope.launch {
            when (val r = authRepo.resetPassword(email, code, newPassword)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, successMessage = r.data, done = true)
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(loading = false, error = ErrorMessages.friendly(r)) }
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
                return AuthViewModel(
                    authRepo = app.authRepository,
                    userRepo = app.userRepository,
                    deviceTokenRepo = app.deviceTokenRepository,
                    socket = app.socket
                ) as T
            }
        }
    }
}
