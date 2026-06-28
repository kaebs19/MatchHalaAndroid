package com.chathala.hala.core.network

import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.core.storage.UserStorage
import com.chathala.hala.feature.auth.data.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator — عند استلام 401 من السيرفر:
 *  1. يقرأ refreshToken المحفوظ
 *  2. يستدعي POST /api/auth/refresh-token
 *  3. ينجح → يحفظ الرموز الجديدة ويُعيد الطلب الأصلي بـ header محدّث
 *  4. يفشل → يمسح الجلسة ويُرجع null (OkHttp يتوقف عن المحاولة)
 *
 * مسح الجلسة يُطلق تلقائياً UI للعودة لـ Login (عبر tokenStorage.token Flow).
 */
class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage,
    private val refreshService: RefreshApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // تجنّب الحلقة اللانهائية
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null
        // استثنِ نفس طلب الـ refresh من هذا الـ authenticator
        if (response.request.url.encodedPath.endsWith("/api/auth/refresh-token")) return null

        val refreshToken = runBlocking { tokenStorage.refreshToken.first() } ?: run {
            clearSession()
            return null
        }

        val newAccessToken = try {
            runBlocking {
                val r = refreshService.refreshToken(RefreshTokenRequest(refreshToken))
                val newAccess = r.data?.token
                val newRefresh = r.data?.refreshToken
                if (newAccess != null) {
                    tokenStorage.save(newAccess, newRefresh)
                    newAccess
                } else null
            }
        } catch (e: Exception) {
            null
        }

        if (newAccessToken == null) {
            clearSession()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun clearSession() {
        runBlocking {
            tokenStorage.clear()
            userStorage.clear()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}
