package com.chathala.hala.feature.push.data

import android.os.Build
import android.util.Log
import com.chathala.hala.BuildConfig
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * تسجيل/إلغاء تسجيل FCM token مع الباك إند.
 *
 *  - register(): يجلب التوكن من Firebase ويرسله للسيرفر
 *  - unregister(): يُلغي التسجيل (يُستخدم عند Logout)
 *  - ensureSynced(): يتم استدعاؤها عند Login/بدء التطبيق للتأكد من وجود توكن صالح
 *
 * التسجيل يتم أيضاً من `HalaMessagingService.onNewToken` عند دوران التوكن.
 */
class DeviceTokenRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    /** ينشر التوكن الحالي (المُعطى) إلى السيرفر. */
    suspend fun register(fcmToken: String): NetworkResult<Unit> = safeApiCall {
        val bearer = bearerOrThrow()
        api.registerDeviceToken(
            bearer = bearer,
            body = DeviceTokenRequest(
                deviceToken = fcmToken,
                platform = "android",
                osVersion = Build.VERSION.RELEASE,
                appVersion = BuildConfig.VERSION_NAME
            )
        )
        Unit
    }

    /**
     * يجلب التوكن الحالي من Firebase ثم يُسجّله.
     * يُستدعى بعد Login أو عند بدء التطبيق إذا كان المستخدم مسجّل الدخول.
     */
    suspend fun ensureSynced(): NetworkResult<String> = safeApiCall {
        val fcm = fetchFcmToken()
        Log.d(TAG, "FCM token: $fcm")
        val bearer = bearerOrThrow()
        api.registerDeviceToken(
            bearer = bearer,
            body = DeviceTokenRequest(
                deviceToken = fcm,
                platform = "android",
                osVersion = Build.VERSION.RELEASE,
                appVersion = BuildConfig.VERSION_NAME
            )
        )
        Log.d(TAG, "FCM registered with backend")
        fcm
    }

    /** يُلغي التسجيل من السيرفر — يُستدعى قبل Logout. */
    suspend fun unregister(): NetworkResult<Unit> = safeApiCall {
        val bearer = bearerOrThrow()
        api.unregisterDeviceToken(bearer)
        // محلياً: احذف توكن FCM أيضاً حتى لا يُعاد استخدامه لحساب آخر
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            .onFailure { Log.w(TAG, "deleteToken() failed: ${it.message}") }
        Unit
    }

    private suspend fun fetchFcmToken(): String = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (!cont.isActive) return@addOnSuccessListener
                if (token.isNullOrBlank()) {
                    cont.resumeWithException(IllegalStateException("FCM token فارغ"))
                } else {
                    cont.resume(token)
                }
            }
            .addOnFailureListener { err ->
                if (cont.isActive) cont.resumeWithException(err)
            }
    }

    private suspend fun bearerOrThrow(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }

    /** محول Task → suspend (للـ Firebase deleteToken). */
    private suspend fun com.google.android.gms.tasks.Task<Void>.await() =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }

    private companion object {
        const val TAG = "DeviceTokenRepo"
    }
}
