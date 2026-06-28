package com.chathala.hala.core.network

import com.chathala.hala.feature.auth.data.RefreshTokenRequest
import com.chathala.hala.feature.auth.data.RefreshTokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API service مخصّص لتجديد الرموز — يعمل بـ OkHttpClient منفصل بدون Authenticator
 * لتفادي حلقة لانهائية.
 */
interface RefreshApiService {

    @POST("api/auth/refresh-token")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): RefreshTokenResponse
}
