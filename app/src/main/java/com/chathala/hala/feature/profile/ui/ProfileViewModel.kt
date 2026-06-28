package com.chathala.hala.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.auth.data.AuthRepository
import com.chathala.hala.feature.profile.data.ProfileRepository
import com.chathala.hala.feature.user.data.User
import com.chathala.hala.feature.user.data.UserRepository
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
import okhttp3.MultipartBody

/**
 * ViewModel لتبويب الملف الشخصي:
 *  - `user` يتابع المستخدم المحفوظ
 *  - `uploading` حالة رفع الصورة
 *  - `refresh()` إعادة الجلب من السيرفر
 *  - `uploadPhoto(part)` رفع صورة جديدة
 *  - `logout()` يمسح الجلسة
 */
class ProfileViewModel(
    private val userRepo: UserRepository,
    private val profileRepo: ProfileRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    val user: StateFlow<User?> = userRepo.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        refresh()
    }

    /** تحميل صامت بدون مؤشر — يُستدعى تلقائياً عند دخول الشاشة. */
    fun refresh() {
        viewModelScope.launch {
            when (val r = userRepo.refresh()) {
                is NetworkResult.Success -> { /* صامت */ }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    /** Pull-to-refresh — يعرض مؤشر. */
    fun pullToRefresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            when (val r = userRepo.refresh()) {
                is NetworkResult.Success -> _message.tryEmit("تم التحديث")
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
            _refreshing.value = false
        }
    }

    fun uploadPhoto(part: MultipartBody.Part) {
        if (_uploading.value) return
        _uploading.value = true
        viewModelScope.launch {
            when (val r = profileRepo.uploadProfileImage(part)) {
                is NetworkResult.Success -> {
                    userRepo.refresh()
                    _message.tryEmit("تم تحديث الصورة")
                }
                is NetworkResult.Error ->
                    _message.tryEmit(ErrorMessages.friendly(r))
            }
            _uploading.value = false
        }
    }

    fun deletePhoto() {
        if (_uploading.value) return
        _uploading.value = true
        viewModelScope.launch {
            when (val r = profileRepo.deleteProfileImage()) {
                is NetworkResult.Success -> {
                    userRepo.refresh()
                    _message.tryEmit("تم حذف الصورة")
                }
                is NetworkResult.Error ->
                    _message.tryEmit(ErrorMessages.friendly(r))
            }
            _uploading.value = false
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepo.logout()
            onDone()
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
                return ProfileViewModel(
                    userRepo = app.userRepository,
                    profileRepo = app.profileRepository,
                    authRepo = app.authRepository
                ) as T
            }
        }
    }
}
