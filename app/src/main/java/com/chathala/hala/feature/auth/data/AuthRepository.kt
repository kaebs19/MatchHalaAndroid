package com.chathala.hala.feature.auth.data

import com.chathala.hala.core.i18n.LocaleManager

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import com.chathala.hala.core.device.DeviceIdentity
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.push.data.DeviceTokenRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage,
    private val userRepository: UserRepository,
    private val deviceTokenRepository: DeviceTokenRepository? = null
) {

    suspend fun register(name: String, email: String, password: String): NetworkResult<AuthResponse> =
        safeApiCall {
            val resp = api.register(
                RegisterRequest(
                    name = name.trim(),
                    email = email.trim(),
                    password = password,
                    deviceFingerprint = DeviceIdentity.deviceFingerprint.ifEmpty { null },
                    deviceToken = DeviceIdentity.deviceToken.ifEmpty { null },
                    vendorId = DeviceIdentity.vendorId.ifEmpty { null }
                )
            )
            persistAuth(resp)
            resp
        }

    suspend fun login(email: String, password: String): NetworkResult<AuthResponse> =
        safeApiCall {
            val resp = api.login(
                LoginRequest(
                    email = email.trim(),
                    password = password,
                    deviceFingerprint = DeviceIdentity.deviceFingerprint.ifEmpty { null },
                    deviceToken = DeviceIdentity.deviceToken.ifEmpty { null },
                    vendorId = DeviceIdentity.vendorId.ifEmpty { null },
                    appVersion = DeviceIdentity.appVersion,
                    deviceModel = DeviceIdentity.deviceModel.ifEmpty { null }
                )
            )
            persistAuth(resp)
            resp
        }

    suspend fun googleLogin(idToken: String): NetworkResult<AuthResponse> =
        safeApiCall {
            val resp = api.googleAuth(
                GoogleAuthRequest(
                    idToken = idToken,
                    deviceFingerprint = DeviceIdentity.deviceFingerprint.ifEmpty { null },
                    deviceToken = DeviceIdentity.deviceToken.ifEmpty { null },
                    vendorId = DeviceIdentity.vendorId.ifEmpty { null },
                    deviceInfo = DeviceInfo(
                        osVersion = DeviceIdentity.osVersion,
                        appVersion = DeviceIdentity.appVersion,
                        deviceModel = DeviceIdentity.deviceModel,
                        language = LocaleManager.current.tag
                    )
                )
            )
            persistAuth(resp)
            resp
        }

    suspend fun facebookLogin(accessToken: String): NetworkResult<AuthResponse> =
        safeApiCall {
            val resp = api.facebookAuth(
                FacebookAuthRequest(
                    accessToken = accessToken,
                    deviceFingerprint = DeviceIdentity.deviceFingerprint.ifEmpty { null },
                    deviceToken = DeviceIdentity.deviceToken.ifEmpty { null },
                    vendorId = DeviceIdentity.vendorId.ifEmpty { null },
                    deviceInfo = DeviceInfo(
                        osVersion = DeviceIdentity.osVersion,
                        appVersion = DeviceIdentity.appVersion,
                        deviceModel = DeviceIdentity.deviceModel,
                        language = LocaleManager.current.tag
                    )
                )
            )
            persistAuth(resp)
            resp
        }

    suspend fun forgotPassword(email: String): NetworkResult<String> =
        safeApiCall {
            val resp = api.forgotPassword(ForgotPasswordRequest(email.trim()))
            S.serverOr(resp.message, R.string.auth_code_sent)
        }

    suspend fun resetPassword(email: String, code: String, newPassword: String): NetworkResult<String> =
        safeApiCall {
            val resp = api.resetPassword(ResetPasswordRequest(email.trim(), code.trim(), newPassword))
            S.serverOr(resp.message, R.string.auth_password_changed)
        }

    /** يمسح الجلسة والمستخدم المحفوظ. */
    suspend fun logout() {
        val session = tokenStorage.snapshot() ?: return
        try {
            deviceTokenRepository?.unregister()
        } finally {
            withContext(NonCancellable) { tokenStorage.endSession(session.sessionId) }
        }
    }

    /**
     * يحذف الحساب نهائياً. للمستخدمين العاديين (authProvider=app) يجب تمرير كلمة المرور.
     * للمستخدمين عبر Google/Apple، password يمكن أن يكون null.
     * عند النجاح يمسح الجلسة المحلية.
     */
    suspend fun deleteAccount(password: String?): NetworkResult<String> =
        safeApiCall {
            val session = tokenStorage.snapshot()
                ?: throw IllegalStateException(S.get(R.string.auth_no_active_session))
            val token = session.accessToken
            val resp = api.deleteAccount(
                bearer = "Bearer $token",
                body = DeleteAccountRequest(password = password)
            )
            try {
                if (tokenStorage.snapshot()?.sessionId == session.sessionId) deviceTokenRepository?.unregister()
            } finally {
                withContext(NonCancellable) { tokenStorage.endSession(session.sessionId) }
            }
            S.serverOr(resp.message, R.string.auth_account_deleted)
        }

    private suspend fun persistAuth(resp: AuthResponse) {
        val data = resp.data ?: return
        val token = data.token ?: return
        val session = tokenStorage.save(token = token, refreshToken = data.refreshToken)
        tokenStorage.withSession(session.sessionId) {
            data.user?.let { userRepository.saveFromAuth(it) }
        }
    }
}
