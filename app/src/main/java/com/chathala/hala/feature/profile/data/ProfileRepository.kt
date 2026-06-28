package com.chathala.hala.feature.profile.data

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
        safeApiCall { api.getInterests().data }

    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<String> =
        safeApiCall {
            val token = storage.token.first()
                ?: throw IllegalStateException("لا يوجد جلسة نشطة، سجّل الدخول مجدداً")
            val resp = api.updateProfile(bearer = "Bearer $token", body = request)
            resp.message ?: "تم تحديث البيانات"
        }

    suspend fun uploadProfileImage(part: MultipartBody.Part): NetworkResult<String> =
        safeApiCall {
            val token = storage.token.first()
                ?: throw IllegalStateException("لا يوجد جلسة نشطة، سجّل الدخول مجدداً")
            val resp = api.uploadProfileImage(bearer = "Bearer $token", profileImage = part)
            resp.message ?: "تم رفع الصورة"
        }

    suspend fun deleteProfileImage(): NetworkResult<String> =
        safeApiCall {
            val token = storage.token.first()
                ?: throw IllegalStateException("لا يوجد جلسة نشطة")
            val resp = api.deleteProfileImage(bearer = "Bearer $token")
            resp.message ?: "تم حذف الصورة"
        }
}
