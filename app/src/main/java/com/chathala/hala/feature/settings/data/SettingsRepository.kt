package com.chathala.hala.feature.settings.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

/**
 * يُجمّع جميع استدعاءات شاشات الإعدادات:
 *  - الخصوصية (جلب + تحديث)
 *  - حول التطبيق
 *  - اتصل بنا
 *  - تغيير كلمة المرور
 */
class SettingsRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    suspend fun fetchPrivacySettings(): NetworkResult<PrivacySettingsData> = safeApiCall {
        val resp = api.getPrivacySettings(bearer())
        resp.data ?: throw IllegalStateException("بيانات غير متوفرة")
    }

    suspend fun setShowDistance(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateShowDistance(bearer(), UpdateDistanceRequest(value))
        resp.message ?: "تم التحديث"
    }

    suspend fun setStealthMode(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateStealthMode(bearer(), UpdateStealthRequest(value))
        resp.message ?: "تم التحديث"
    }

    suspend fun setShowAge(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateShowAge(bearer(), UpdateShowAgeRequest(value))
        resp.message ?: "تم التحديث"
    }

    suspend fun setShowCountry(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateShowCountry(bearer(), UpdateShowCountryRequest(value))
        resp.message ?: "تم التحديث"
    }

    suspend fun setAcceptingRequests(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateAcceptingRequests(bearer(), UpdateAcceptingRequestsRequest(value))
        resp.message ?: "تم التحديث"
    }

    suspend fun setPremiumOnlyRequests(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updatePremiumOnlyRequests(bearer(), UpdatePremiumOnlyRequestsRequest(value))
        resp.message ?: "تم التحديث"
    }

    suspend fun setDoNotDisturb(
        enabled: Boolean,
        startHour: Int? = null,
        startMinute: Int? = null,
        endHour: Int? = null,
        endMinute: Int? = null
    ): NetworkResult<DoNotDisturbData> = safeApiCall {
        val resp = api.updateDoNotDisturb(
            bearer(),
            UpdateDoNotDisturbRequest(enabled, startHour, startMinute, endHour, endMinute)
        )
        resp.data ?: throw IllegalStateException("بيانات غير متوفرة")
    }

    suspend fun setPauseDiscovery(
        enabled: Boolean,
        durationHours: Int? = null
    ): NetworkResult<String> = safeApiCall {
        val resp = api.updatePauseDiscovery(bearer(), UpdatePauseDiscoveryRequest(enabled, durationHours))
        resp.message ?: "تم التحديث"
    }

    suspend fun fetchNotificationPrefs(): NetworkResult<NotificationPrefsData> = safeApiCall {
        val resp = api.getNotificationPrefs(bearer())
        resp.data ?: throw IllegalStateException("بيانات غير متوفرة")
    }

    suspend fun updateNotificationPref(
        body: UpdateNotificationPrefRequest
    ): NetworkResult<NotificationPrefsData> = safeApiCall {
        val resp = api.updateNotificationPref(bearer(), body)
        resp.data ?: throw IllegalStateException("بيانات غير متوفرة")
    }

    suspend fun fetchAbout(): NetworkResult<AboutData> = safeApiCall {
        val resp = api.getAbout()
        resp.data ?: throw IllegalStateException("لا توجد معلومات عن التطبيق")
    }

    suspend fun fetchContact(): NetworkResult<ContactData> = safeApiCall {
        val resp = api.getContact()
        resp.data ?: throw IllegalStateException("لا توجد معلومات الاتصال")
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): NetworkResult<String> = safeApiCall {
        val resp = api.changePassword(
            bearer = bearer(),
            body = ChangePasswordRequest(currentPassword, newPassword)
        )
        resp.message ?: "تم تغيير كلمة المرور"
    }

    suspend fun setAllowSensitiveContent(enabled: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateAllowSensitiveContent(bearer(), UpdateAllowSensitiveContentRequest(enabled))
        resp.message ?: if (enabled) "تم التفعيل" else "تم التعطيل"
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
