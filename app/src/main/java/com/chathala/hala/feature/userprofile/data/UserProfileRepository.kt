package com.chathala.hala.feature.userprofile.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

class UserProfileRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {
    suspend fun fetch(userId: String): NetworkResult<UserProfile> = safeApiCall {
        val resp = api.getUserProfile(bearer(), userId)
        resp.data?.user ?: throw IllegalStateException("بيانات البروفايل غير متوفرة")
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
