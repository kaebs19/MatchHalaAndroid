package com.chathala.hala.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.DateUtils
import com.chathala.hala.feature.profile.data.Interest
import com.chathala.hala.feature.profile.data.ProfileRepository
import com.chathala.hala.feature.profile.data.UpdateProfileRequest
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class ProfileCompletionState(
    val interestsLoading: Boolean = false,
    val interests: List<Interest> = emptyList(),
    val interestsError: String? = null,

    val saving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false
)

class ProfileCompletionViewModel(
    private val profileRepo: ProfileRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileCompletionState())
    val state: StateFlow<ProfileCompletionState> = _state.asStateFlow()

    init {
        loadInterests()
    }

    fun loadInterests() {
        _state.update { it.copy(interestsLoading = true, interestsError = null) }
        viewModelScope.launch {
            when (val r = profileRepo.fetchInterests()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(interestsLoading = false, interests = r.data)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(interestsLoading = false, interestsError = r.message)
                }
            }
        }
    }

    fun save(
        context: android.content.Context,
        birthDate: Date,
        gender: String,
        country: String,
        interestKeys: List<String>,
        defaultAvatar: String?,
        uploadUri: android.net.Uri?
    ) {
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            // 1) رفع صورة المستخدم إن اختارها (تُحدّث profileImage على الخادم)
            if (uploadUri != null) {
                val part = com.chathala.hala.core.util.MediaUploadHelper
                    .uriToImagePart(context, uploadUri, fieldName = "profileImage")
                if (part != null) {
                    when (val up = profileRepo.uploadProfileImage(part)) {
                        is NetworkResult.Error -> {
                            _state.update { it.copy(saving = false, saveError = up.message) }
                            return@launch
                        }
                        else -> Unit
                    }
                }
            }

            // 2) تحديث باقي البيانات (مع الأفاتار الجاهز إن اختاره ولم يرفع صورة)
            val req = UpdateProfileRequest(
                birthDate = DateUtils.formatIso(birthDate),
                gender = gender,
                country = country,
                interests = interestKeys,
                defaultAvatar = if (uploadUri == null) defaultAvatar else null
            )
            when (val r = profileRepo.updateProfile(req)) {
                is NetworkResult.Success -> {
                    // ✅ إعادة جلب بيانات المستخدم الكاملة من /api/auth/me
                    userRepo.refresh()
                    _state.update { it.copy(saving = false, saved = true) }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(saving = false, saveError = r.message) }
            }
        }
    }

    fun clearSaved() {
        _state.update { it.copy(saved = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return ProfileCompletionViewModel(
                    profileRepo = app.profileRepository,
                    userRepo = app.userRepository
                ) as T
            }
        }
    }
}
