package com.chathala.hala.feature.suspension.data

import com.chathala.hala.core.device.DeviceIdentity
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

/**
 * يجمّع استدعاءات شاشة الحظر:
 *  - [checkDeviceBan]: جلب تفاصيل حظر الجهاز (مسار عام).
 *  - [submitAccountAppeal]: استئناف موثّق لإيقاف الحساب.
 *  - [submitDeviceBanAppeal]: استئناف عام لحظر الجهاز.
 */
class SuspensionRepository(
    private val tokenStorage: TokenStorage,
    private val api: ApiService = ApiClient.service
) {

    suspend fun checkDeviceBan(): NetworkResult<DeviceBanData> = safeApiCall {
        val resp = api.checkDeviceBan(
            CheckDeviceBanRequest(
                deviceFingerprint = DeviceIdentity.deviceFingerprint.ifEmpty { null },
                deviceToken = DeviceIdentity.deviceToken.ifEmpty { null },
                vendorId = DeviceIdentity.vendorId.ifEmpty { null }
            )
        )
        resp.data ?: throw IllegalStateException("تعذّر جلب حالة الجهاز")
    }

    /**
     * @param token توكن الإيقاف القصير من رد تسجيل الدخول (تعليق وقت الدخول).
     *              عند التعليق أثناء الجلسة يكون null فنستخدم توكن الجلسة المخزّن.
     * @param permanent true → الإيقاف دائم (actionType = "ban")، وإلا "suspension".
     */
    suspend fun submitAccountAppeal(
        reason: String,
        permanent: Boolean,
        token: String?
    ): NetworkResult<String> = safeApiCall {
        val bearerToken = token?.takeIf { it.isNotBlank() } ?: tokenStorage.token.first()
        if (bearerToken.isNullOrBlank()) {
            throw IllegalStateException("انتهت جلسة الاستئناف — أعد تسجيل الدخول وحاول مجدداً")
        }
        val resp = api.submitAppeal(
            bearer = "Bearer $bearerToken",
            body = AppealRequest(
                reason = reason.trim(),
                actionType = if (permanent) "ban" else "suspension"
            )
        )
        resp.message ?: "تم إرسال الاستئناف"
    }

    /** يجلب الاستئنافات السابقة للمستخدم (وضع الحساب فقط — يحتاج توكن). */
    suspend fun fetchMyAppeals(
        token: String?
    ): NetworkResult<List<com.chathala.hala.feature.settings.data.AppealItem>> = safeApiCall {
        val bearerToken = token?.takeIf { it.isNotBlank() } ?: tokenStorage.token.first()
        if (bearerToken.isNullOrBlank()) return@safeApiCall emptyList()
        api.getMyAppeals("Bearer $bearerToken").data
    }

    suspend fun submitDeviceBanAppeal(reason: String, email: String?): NetworkResult<String> = safeApiCall {
        val resp = api.submitPublicDeviceBanAppeal(
            PublicDeviceBanAppealRequest(
                reason = reason.trim(),
                email = email?.trim()?.ifEmpty { null },
                deviceFingerprint = DeviceIdentity.deviceFingerprint.ifEmpty { null },
                deviceToken = DeviceIdentity.deviceToken.ifEmpty { null }
            )
        )
        resp.message ?: "تم إرسال الاستئناف"
    }
}
