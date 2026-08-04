package com.chathala.hala.feature.settings.data

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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
        resp.data ?: throw IllegalStateException(S.get(R.string.err_data_unavailable))
    }

    suspend fun setShowDistance(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateShowDistance(bearer(), UpdateDistanceRequest(value))
        S.serverOr(resp.message, R.string.conv_updated)
    }

    suspend fun setStealthMode(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateStealthMode(bearer(), UpdateStealthRequest(value))
        S.serverOr(resp.message, R.string.conv_updated)
    }

    suspend fun setShowAge(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateShowAge(bearer(), UpdateShowAgeRequest(value))
        S.serverOr(resp.message, R.string.conv_updated)
    }

    suspend fun setShowCountry(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateShowCountry(bearer(), UpdateShowCountryRequest(value))
        S.serverOr(resp.message, R.string.conv_updated)
    }

    suspend fun setAcceptingRequests(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateAcceptingRequests(bearer(), UpdateAcceptingRequestsRequest(value))
        S.serverOr(resp.message, R.string.conv_updated)
    }

    suspend fun setPremiumOnlyRequests(value: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updatePremiumOnlyRequests(bearer(), UpdatePremiumOnlyRequestsRequest(value))
        S.serverOr(resp.message, R.string.conv_updated)
    }

    /** من يستطيع إرسال طلب صداقة لي: everyone | contacts | nobody. */
    suspend fun setFriendRequests(value: String): NetworkResult<String> = safeApiCall {
        val resp = api.updateFriendRequestsPrivacy(
            bearer(),
            com.chathala.hala.feature.friends.data.UpdateFriendRequestsPrivacyBody(value)
        )
        S.serverOr(resp.message, R.string.conv_updated)
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
        resp.data ?: throw IllegalStateException(S.get(R.string.err_data_unavailable))
    }

    suspend fun setPauseDiscovery(
        enabled: Boolean,
        durationHours: Int? = null
    ): NetworkResult<String> = safeApiCall {
        val resp = api.updatePauseDiscovery(bearer(), UpdatePauseDiscoveryRequest(enabled, durationHours))
        S.serverOr(resp.message, R.string.conv_updated)
    }

    suspend fun fetchNotificationPrefs(): NetworkResult<NotificationPrefsData> = safeApiCall {
        val resp = api.getNotificationPrefs(bearer())
        resp.data ?: throw IllegalStateException(S.get(R.string.err_data_unavailable))
    }

    suspend fun updateNotificationPref(
        body: UpdateNotificationPrefRequest
    ): NetworkResult<NotificationPrefsData> = safeApiCall {
        val resp = api.updateNotificationPref(bearer(), body)
        resp.data ?: throw IllegalStateException(S.get(R.string.err_data_unavailable))
    }

    suspend fun fetchAbout(): NetworkResult<AboutData> = safeApiCall {
        val resp = api.getAbout()
        resp.data ?: throw IllegalStateException(S.get(R.string.settings_no_app_info))
    }

    suspend fun fetchContact(): NetworkResult<ContactData> = safeApiCall {
        val resp = api.getContact()
        resp.data ?: throw IllegalStateException(S.get(R.string.settings_no_contact_info))
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): NetworkResult<String> = safeApiCall {
        val resp = api.changePassword(
            bearer = bearer(),
            body = ChangePasswordRequest(currentPassword, newPassword)
        )
        S.serverOr(resp.message, R.string.auth_password_changed)
    }

    suspend fun setAllowSensitiveContent(enabled: Boolean): NetworkResult<String> = safeApiCall {
        val resp = api.updateAllowSensitiveContent(bearer(), UpdateAllowSensitiveContentRequest(enabled))
        resp.message ?: if (enabled) S.get(R.string.settings_enabled_toast) else S.get(R.string.settings_disabled_toast)
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException(S.get(R.string.auth_no_active_session))
        return "Bearer $token"
    }
}
