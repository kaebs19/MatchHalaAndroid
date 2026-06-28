package com.chathala.hala.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.DateUtils
import com.chathala.hala.feature.profile.data.Interest
import com.chathala.hala.feature.profile.data.ProfileRepository
import com.chathala.hala.feature.profile.data.UpdateProfileRequest
import com.chathala.hala.feature.user.data.User
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class EditProfileState(
    val loadingInterests: Boolean = false,
    val availableInterests: List<Interest> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val uploading: Boolean = false,
    val error: String? = null,

    val name: String = "",
    val bio: String = "",
    val gender: String? = null,
    val birthDate: Date? = null,
    val country: String? = null,
    val interests: Set<String> = emptySet(),
    val hasImage: Boolean = false,
    val hydrated: Boolean = false,

    val initialName: String = "",
    val initialBio: String = "",
    val initialGender: String? = null,
    val initialBirthDate: Date? = null,
    val initialCountry: String? = null,
    val initialInterests: Set<String> = emptySet()
) {
    val hasUnsavedChanges: Boolean
        get() = hydrated && (
            name != initialName ||
            bio != initialBio ||
            gender != initialGender ||
            birthDate != initialBirthDate ||
            country != initialCountry ||
            interests != initialInterests
        )
}

class EditProfileViewModel(
    private val profileRepo: ProfileRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        loadInterests()
        loadCurrentUser()
    }

    private fun loadInterests() {
        _state.update { it.copy(loadingInterests = true) }
        viewModelScope.launch {
            when (val r = profileRepo.fetchInterests()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loadingInterests = false, availableInterests = r.data)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loadingInterests = false)
                }
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepo.currentUser.collect { user ->
                if (user != null && !_state.value.hydrated) {
                    val birth = parseIso(user.birthDate)
                    val interests = user.interests.toSet()
                    _state.update { it.copy(
                        name = user.name,
                        bio = user.bio.orEmpty(),
                        gender = user.gender,
                        birthDate = birth,
                        country = user.country,
                        interests = interests,
                        hasImage = !user.profileImage.isNullOrBlank(),
                        initialName = user.name,
                        initialBio = user.bio.orEmpty(),
                        initialGender = user.gender,
                        initialBirthDate = birth,
                        initialCountry = user.country,
                        initialInterests = interests,
                        hydrated = true
                    ) }
                } else if (user != null) {
                    // مراقبة تغيير الصورة فقط بعد الـ hydration الأولي
                    _state.update { it.copy(hasImage = !user.profileImage.isNullOrBlank()) }
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setBio(v: String) = _state.update { it.copy(bio = v) }
    fun setGender(v: String) = _state.update { it.copy(gender = v) }
    fun setBirthDate(v: Date) = _state.update { it.copy(birthDate = v) }
    fun setCountry(v: String) = _state.update { it.copy(country = v) }
    fun toggleInterest(key: String) = _state.update {
        val next = if (key in it.interests) it.interests - key else it.interests + key
        it.copy(interests = next)
    }

    fun uploadPhoto(part: MultipartBody.Part) {
        if (_state.value.uploading) return
        _state.update { it.copy(uploading = true) }
        viewModelScope.launch {
            when (val r = profileRepo.uploadProfileImage(part)) {
                is NetworkResult.Success -> {
                    userRepo.refresh()
                    _message.tryEmit("تم تحديث الصورة")
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
            _state.update { it.copy(uploading = false) }
        }
    }

    fun deletePhoto() {
        if (_state.value.uploading) return
        _state.update { it.copy(uploading = true) }
        viewModelScope.launch {
            when (val r = profileRepo.deleteProfileImage()) {
                is NetworkResult.Success -> {
                    userRepo.refresh()
                    _message.tryEmit("تم حذف الصورة")
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
            _state.update { it.copy(uploading = false) }
        }
    }

    fun save() {
        val s = _state.value
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val req = UpdateProfileRequest(
                name = s.name.trim().takeIf { it.isNotBlank() },
                bio = s.bio.trim().takeIf { it.isNotBlank() } ?: "",
                gender = s.gender,
                birthDate = s.birthDate?.let { DateUtils.formatIso(it) },
                country = s.country,
                interests = s.interests.toList()
            )
            when (val r = profileRepo.updateProfile(req)) {
                is NetworkResult.Success -> {
                    userRepo.refresh()
                    _state.update { it.copy(saving = false, saved = true) }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(saving = false, error = ErrorMessages.friendly(r)) }
            }
        }
    }

    fun clearSaved() = _state.update { it.copy(saved = false) }

    private fun parseIso(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        val trimmed = iso.substringBefore('.').trimEnd('Z')
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = true
            }.parse(trimmed)
        }.getOrNull()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return EditProfileViewModel(
                    profileRepo = app.profileRepository,
                    userRepo = app.userRepository
                ) as T
            }
        }
    }
}
