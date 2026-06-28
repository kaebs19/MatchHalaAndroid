package com.chathala.hala.feature.suspension.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.suspension.data.AccountSuspensionInfo
import com.chathala.hala.feature.suspension.data.DeviceBanData
import com.chathala.hala.feature.suspension.data.SuspensionGate
import com.chathala.hala.feature.suspension.data.SuspensionMode
import com.chathala.hala.feature.suspension.data.SuspensionRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SuspendedUiState(
    val mode: SuspensionMode,
    val loadingDetails: Boolean = false,
    val detailsError: String? = null,
    val account: AccountSuspensionInfo? = null,
    val device: DeviceBanData? = null,
    val canAppeal: Boolean = true,
    val submitting: Boolean = false,
    val appealError: String? = null,
    val appealSent: Boolean = false,
    val appealSentMessage: String? = null,
    // ── إعادة التحقق من رفع التعليق ──
    val rechecking: Boolean = false,
    val recheckMessage: String? = null,
    val lifted: Boolean = false,
    // ── الاستئنافات السابقة (وضع الحساب) ──
    val previousAppeals: List<com.chathala.hala.feature.settings.data.AppealItem> = emptyList()
)

class SuspendedViewModel(
    private val mode: SuspensionMode,
    private val repo: SuspensionRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        SuspendedUiState(
            mode = mode,
            account = if (mode == SuspensionMode.ACCOUNT) SuspensionGate.consume() else null
        )
    )
    val state: StateFlow<SuspendedUiState> = _state.asStateFlow()

    init {
        when (mode) {
            SuspensionMode.DEVICE -> loadDeviceBan()
            SuspensionMode.ACCOUNT -> {
                enrichFromCachedUser()
                loadPreviousAppeals()
            }
        }
    }

    /** يجلب الاستئنافات السابقة لعرضها (وضع الحساب فقط). */
    private fun loadPreviousAppeals() {
        viewModelScope.launch {
            val token = _state.value.account?.token
            when (val r = repo.fetchMyAppeals(token)) {
                is NetworkResult.Success -> _state.update { it.copy(previousAppeals = r.data) }
                is NetworkResult.Error -> { /* غير حرج */ }
            }
        }
    }

    /** يملأ الاسم/البريد/المعرّف من المستخدم المخزّن (رد التعليق أثناء الجلسة لا يحملها). */
    private fun enrichFromCachedUser() {
        viewModelScope.launch {
            val acc = _state.value.account ?: return@launch
            if (!acc.name.isNullOrBlank() && !acc.email.isNullOrBlank() && !acc.userId.isNullOrBlank()) return@launch
            val user = runCatching { userRepo.currentUser.first() }.getOrNull() ?: return@launch
            _state.update {
                val a = it.account ?: return@update it
                it.copy(
                    account = a.copy(
                        name = a.name?.takeIf { n -> n.isNotBlank() } ?: user.name,
                        email = a.email?.takeIf { e -> e.isNotBlank() } ?: user.email,
                        userId = a.userId?.takeIf { id -> id.isNotBlank() } ?: user.id
                    )
                )
            }
        }
    }

    fun loadDeviceBan() {
        _state.update { it.copy(loadingDetails = true, detailsError = null) }
        viewModelScope.launch {
            when (val r = repo.checkDeviceBan()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        loadingDetails = false,
                        device = r.data,
                        canAppeal = r.data.canAppeal
                    )
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loadingDetails = false, detailsError = r.message)
                }
            }
        }
    }

    fun clearAppealFeedback() {
        _state.update { it.copy(appealError = null) }
    }

    fun clearRecheckMessage() {
        _state.update { it.copy(recheckMessage = null) }
    }

    /**
     * يتحقق ما إذا رُفِع التعليق: يستدعي /api/auth/me (الذي يرفع التعليق المؤقت تلقائياً
     * عند انتهاء مدته ويحدّث بيانات المستخدم). عند النجاح → [SuspendedUiState.lifted].
     *
     * @param showFeedback إظهار رسالة "لا يزال معلّقاً" (للضغط اليدوي فقط، لا للتحقق التلقائي).
     */
    fun recheck(showFeedback: Boolean) {
        if (mode != SuspensionMode.ACCOUNT) {
            loadDeviceBan()
            return
        }
        if (_state.value.rechecking || _state.value.lifted) return
        _state.update { it.copy(rechecking = true, recheckMessage = null) }
        viewModelScope.launch {
            when (val r = userRepo.refresh()) {
                is NetworkResult.Success -> _state.update { it.copy(rechecking = false, lifted = true) }
                is NetworkResult.Error -> _state.update {
                    it.copy(
                        rechecking = false,
                        recheckMessage = if (showFeedback) {
                            if (r.code == "ACCOUNT_SUSPENDED") "لا يزال حسابك معلّقاً — حاول لاحقاً"
                            else r.message
                        } else null
                    )
                }
            }
        }
    }

    /**
     * @param email يُستخدم فقط في وضع حظر الجهاز (المسار العام).
     */
    fun submitAppeal(reason: String, email: String?) {
        if (reason.isBlank()) {
            _state.update { it.copy(appealError = "يُرجى كتابة سبب الاستئناف") }
            return
        }
        _state.update { it.copy(submitting = true, appealError = null) }
        viewModelScope.launch {
            val result = when (mode) {
                SuspensionMode.ACCOUNT -> repo.submitAccountAppeal(
                    reason = reason,
                    permanent = _state.value.account?.isPermanent ?: false,
                    token = _state.value.account?.token
                )
                SuspensionMode.DEVICE -> repo.submitDeviceBanAppeal(reason = reason, email = email)
            }
            when (result) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(submitting = false, appealSent = true, appealSentMessage = result.data)
                    }
                    if (mode == SuspensionMode.ACCOUNT) loadPreviousAppeals()
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(submitting = false, appealError = result.message)
                }
            }
        }
    }

    class Factory(private val mode: SuspensionMode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
            return SuspendedViewModel(
                mode = mode,
                repo = app.suspensionRepository,
                userRepo = app.userRepository
            ) as T
        }
    }
}
