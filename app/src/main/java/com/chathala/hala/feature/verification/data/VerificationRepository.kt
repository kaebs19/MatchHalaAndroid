package com.chathala.hala.feature.verification.data

import android.content.Context
import android.net.Uri
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.core.util.MediaUploadHelper
import kotlinx.coroutines.flow.first

class VerificationRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {
    suspend fun fetchStatus(): NetworkResult<VerificationStatusData> = safeApiCall {
        val resp = api.getVerificationStatus(bearer())
        resp.data ?: VerificationStatusData(status = "none", isVerified = false)
    }

    suspend fun submit(context: Context, selfieUri: Uri): NetworkResult<String> = safeApiCall {
        val part = MediaUploadHelper.uriToImagePart(context, selfieUri, fieldName = "selfie")
            ?: throw IllegalStateException("تعذّر قراءة الصورة")
        val resp = api.submitVerification(bearer(), part)
        resp.message ?: "تم إرسال الطلب"
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
