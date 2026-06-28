package com.chathala.hala.feature.auth.data

import android.util.Log
import com.chathala.hala.core.device.DeviceIdentity
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.chats.data.ChatsCacheStorage
import com.chathala.hala.feature.chats.socket.HalaSocket
import com.chathala.hala.feature.discover.data.DiscoverCacheStorage
import com.chathala.hala.feature.push.data.DeviceTokenRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage,
    private val userRepository: UserRepository,
    private val deviceTokenRepository: DeviceTokenRepository? = null,
    private val socket: HalaSocket? = null,
    private val discoverCache: DiscoverCacheStorage? = null,
    private val chatsCache: ChatsCacheStorage? = null
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
                        deviceModel = DeviceIdentity.deviceModel
                    )
                )
            )
            persistAuth(resp)
            resp
        }

    suspend fun forgotPassword(email: String): NetworkResult<String> =
        safeApiCall {
            val resp = api.forgotPassword(ForgotPasswordRequest(email.trim()))
            resp.message ?: "تم إرسال رمز التحقق"
        }

    suspend fun resetPassword(email: String, code: String, newPassword: String): NetworkResult<String> =
        safeApiCall {
            val resp = api.resetPassword(ResetPasswordRequest(email.trim(), code.trim(), newPassword))
            resp.message ?: "تم تغيير كلمة المرور"
        }

    /** يمسح الجلسة والمستخدم المحفوظ. */
    suspend fun logout() {
        // حاول إلغاء FCM قبل مسح التوكن (يحتاج bearer)
        runCatching { deviceTokenRepository?.unregister() }
            .onFailure { Log.w("AuthRepository", "FCM unregister failed: ${it.message}") }
        runCatching { socket?.disconnect() }
        runCatching { discoverCache?.clear() }
        runCatching { chatsCache?.clear() }
        tokenStorage.clear()
        userRepository.clear()
    }

    /**
     * يحذف الحساب نهائياً. للمستخدمين العاديين (authProvider=app) يجب تمرير كلمة المرور.
     * للمستخدمين عبر Google/Apple، password يمكن أن يكون null.
     * عند النجاح يمسح الجلسة المحلية.
     */
    suspend fun deleteAccount(password: String?): NetworkResult<String> =
        safeApiCall {
            val token = tokenStorage.token.first()
                ?: throw IllegalStateException("لا يوجد جلسة نشطة")
            val resp = api.deleteAccount(
                bearer = "Bearer $token",
                body = DeleteAccountRequest(password = password)
            )
            runCatching { deviceTokenRepository?.unregister() }
                .onFailure { Log.w("AuthRepository", "FCM unregister failed: ${it.message}") }
            runCatching { socket?.disconnect() }
            runCatching { discoverCache?.clear() }
        runCatching { chatsCache?.clear() }
            tokenStorage.clear()
            userRepository.clear()
            resp.message ?: "تم حذف الحساب"
        }

    private suspend fun persistAuth(resp: AuthResponse) {
        val data = resp.data ?: return
        val token = data.token ?: return
        tokenStorage.save(token = token, refreshToken = data.refreshToken)
        data.user?.let { userRepository.saveFromAuth(it) }
    }
}
