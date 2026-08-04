package com.chathala.hala.feature.profile.data

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first
import okhttp3.MultipartBody

class ProfileRepository(
    private val api: ApiService = ApiClient.service,
    private val storage: TokenStorage
) {

    suspend fun fetchInterests(): NetworkResult<List<Interest>> =
        safeApiCall {
            api.getInterests().data.also { InterestsCatalog.update(it) }
        }

    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<String> =
        safeApiCall {
            val token = storage.token.first()
                ?: throw IllegalStateException(S.get(R.string.err_no_session_signin))
            val resp = api.updateProfile(bearer = "Bearer $token", body = request)
            resp.message ?: S.get(R.string.profile_data_updated)
        }

    suspend fun uploadProfileImage(part: MultipartBody.Part): NetworkResult<String> =
        safeApiCall {
            val token = storage.token.first()
                ?: throw IllegalStateException(S.get(R.string.err_no_session_signin))
            val resp = api.uploadProfileImage(bearer = "Bearer $token", profileImage = part)
            resp.message ?: S.get(R.string.profile_photo_uploaded)
        }

    suspend fun deleteProfileImage(): NetworkResult<String> =
        safeApiCall {
            val token = storage.token.first()
                ?: throw IllegalStateException(S.get(R.string.auth_no_active_session))
            val resp = api.deleteProfileImage(bearer = "Bearer $token")
            resp.message ?: S.get(R.string.profile_photo_deleted)
        }
}
