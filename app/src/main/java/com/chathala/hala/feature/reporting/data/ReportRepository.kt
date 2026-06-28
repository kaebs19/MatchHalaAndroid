package com.chathala.hala.feature.reporting.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

class ReportRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {
    suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        description: String? = null
    ): NetworkResult<String> = safeApiCall {
        val resp = api.reportUser(
            bearer = bearer(),
            body = CreateReportRequest(
                reportedUser = userId,
                reason = reason.apiValue,
                description = description?.takeIf { it.isNotBlank() }
            )
        )
        resp.message ?: "تم إرسال البلاغ"
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
