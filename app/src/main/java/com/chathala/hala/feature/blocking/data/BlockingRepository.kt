package com.chathala.hala.feature.blocking.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

class BlockingRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {
    suspend fun fetchBlocked(): NetworkResult<List<BlockedUser>> = safeApiCall {
        val resp = api.getBlockedUsers(bearer())
        resp.data?.blockedUsers ?: emptyList()
    }

    suspend fun block(userId: String): NetworkResult<String> = safeApiCall {
        val resp = api.blockUser(bearer(), userId)
        resp.message ?: "تم حظر المستخدم"
    }

    suspend fun unblock(userId: String): NetworkResult<String> = safeApiCall {
        val resp = api.unblockUser(bearer(), userId)
        resp.message ?: "تم إلغاء الحظر"
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
