package com.chathala.hala.core.network

import com.chathala.hala.core.session.SessionTokenStore
import com.chathala.hala.core.session.SessionTokens
import com.chathala.hala.feature.auth.data.RefreshTokenRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.io.IOException

/** Serializes refreshes and never replays a request under another account's session. */
class TokenAuthenticator(
    private val tokenStorage: SessionTokenStore,
    private val refreshService: RefreshApiService
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        if (response.request.url.encodedPath.endsWith("/api/auth/refresh-token")) return null
        val sent = response.request.tag(SessionTokens::class.java) ?: return null
        if (response.request.header("Authorization") != "Bearer ${sent.accessToken}") return null

        return synchronized(refreshLock) {
            runBlocking {
                val current = tokenStorage.snapshot() ?: return@runBlocking null
                if (current.sessionId != sent.sessionId) return@runBlocking null
                // A parallel request has already renewed this session.
                if (current.accessToken != sent.accessToken) {
                    return@runBlocking retry(response, current)
                }
                val refreshToken = current.refreshToken?.takeIf { it.isNotBlank() } ?: run {
                    tokenStorage.clearIfCurrent(current)
                    return@runBlocking null
                }
                val refreshed = try {
                    refreshService.refreshToken(RefreshTokenRequest(refreshToken))
                } catch (e: HttpException) {
                    // Only an explicit authentication rejection invalidates a session.
                    if (e.code() == 401 || e.code() == 403) tokenStorage.clearIfCurrent(current)
                    return@runBlocking null
                } catch (_: IOException) {
                    return@runBlocking null
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Malformed responses also must not destroy a valid refresh token.
                    return@runBlocking null
                }
                val access = refreshed.data?.token?.takeIf { it.isNotBlank() }
                if (!refreshed.success || access == null) return@runBlocking null
                if (!tokenStorage.replaceIfCurrent(current, access, refreshed.data?.refreshToken)) {
                    return@runBlocking null
                }
                retry(response, current.copy(accessToken = access, refreshToken = refreshed.data?.refreshToken ?: refreshToken))
            }
        }
    }

    private fun retry(response: Response, tokens: SessionTokens): Request = response.request.newBuilder()
        .header("Authorization", "Bearer ${tokens.accessToken}")
        .tag(SessionTokens::class.java, tokens)
        .build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}
