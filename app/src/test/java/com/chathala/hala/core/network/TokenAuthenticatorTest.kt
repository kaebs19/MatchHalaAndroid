package com.chathala.hala.core.network

import com.chathala.hala.core.session.SessionTokenStore
import com.chathala.hala.core.session.SessionTokens
import com.chathala.hala.feature.auth.data.RefreshTokenData
import com.chathala.hala.feature.auth.data.RefreshTokenRequest
import com.chathala.hala.feature.auth.data.RefreshTokenResponse
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {
    private val original = SessionTokens("old-access", "refresh", "account-A-session")

    private class Store(initial: SessionTokens) : SessionTokenStore {
        @Volatile var current: SessionTokens? = initial
        var clears = 0
        override suspend fun snapshot() = current
        override suspend fun replaceIfCurrent(expected: SessionTokens, accessToken: String, refreshToken: String?): Boolean {
            if (current != expected) return false
            current = expected.copy(accessToken = accessToken, refreshToken = refreshToken ?: expected.refreshToken)
            return true
        }
        override suspend fun clearIfCurrent(expected: SessionTokens): Boolean {
            if (current != expected) return false
            clears++
            current = null
            return true
        }
    }

    private fun response(tokens: SessionTokens? = original, header: Boolean = true): Response {
        val request = Request.Builder().url("https://example.test/api/messages")
            .tag(SessionTokens::class.java, tokens)
            .apply { if (header) header("Authorization", "Bearer ${tokens?.accessToken}") }
            .build()
        return Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
    }

    private fun api(block: suspend () -> RefreshTokenResponse) = object : RefreshApiService {
        override suspend fun refreshToken(body: RefreshTokenRequest) = block()
    }

    private fun success(refresh: String? = "new-refresh") = RefreshTokenResponse(true, RefreshTokenData("new-access", refresh))
    private fun httpError(code: Int) = HttpException(retrofit2.Response.error<Any>(code, "{}".toResponseBody()))

    @Test fun `temporary network failure preserves the session`() {
        val store = Store(original)
        assertNull(TokenAuthenticator(store, api { throw IOException("offline") }).authenticate(null, response()))
        assertEquals(original, store.current)
        assertEquals(0, store.clears)
    }

    @Test fun `server failure preserves the session`() {
        val store = Store(original)
        assertNull(TokenAuthenticator(store, api { throw httpError(503) }).authenticate(null, response()))
        assertEquals(original, store.current)
    }

    @Test fun `explicit refresh rejection clears the matching session`() {
        val store = Store(original)
        assertNull(TokenAuthenticator(store, api { throw httpError(401) }).authenticate(null, response()))
        assertNull(store.current)
        assertEquals(1, store.clears)
    }

    @Test fun `malformed successful response does not delete credentials`() {
        val store = Store(original)
        assertNull(TokenAuthenticator(store, api { RefreshTokenResponse(true) }).authenticate(null, response()))
        assertEquals(original, store.current)
    }

    @Test fun `rotation preserves refresh token if server omits a replacement`() {
        val store = Store(original)
        val request = TokenAuthenticator(store, api { success(null) }).authenticate(null, response())
        assertEquals("Bearer new-access", request?.header("Authorization"))
        assertEquals("refresh", store.current?.refreshToken)
        assertEquals(original.sessionId, store.current?.sessionId)
    }

    @Test fun `concurrent unauthorized requests perform only one refresh`() {
        val store = Store(original)
        val calls = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val authenticator = TokenAuthenticator(store, api {
            calls.incrementAndGet()
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            success()
        })
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit<Request?> { authenticator.authenticate(null, response()) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val second = executor.submit<Request?> { authenticator.authenticate(null, response()) }
            release.countDown()
            assertEquals("Bearer new-access", first.get(5, TimeUnit.SECONDS)?.header("Authorization"))
            assertEquals("Bearer new-access", second.get(5, TimeUnit.SECONDS)?.header("Authorization"))
            assertEquals(1, calls.get())
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test fun `account switch while refreshing cannot overwrite new credentials`() {
        val store = Store(original)
        val other = SessionTokens("B-access", "B-refresh", "account-B-session")
        val request = TokenAuthenticator(store, api { store.current = other; success() }).authenticate(null, response())
        assertNull(request)
        assertEquals(other, store.current)
    }

    @Test fun `rejected old refresh cannot clear the new account`() {
        val store = Store(original)
        val other = SessionTokens("B-access", "B-refresh", "account-B-session")
        val request = TokenAuthenticator(store, api { store.current = other; throw httpError(401) }).authenticate(null, response())
        assertNull(request)
        assertEquals(other, store.current)
        assertEquals(0, store.clears)
    }

    @Test fun `late unauthorized response never retries under another account`() {
        val store = Store(original.copy(sessionId = "new-login"))
        val request = TokenAuthenticator(store, api { error("must not refresh") }).authenticate(null, response())
        assertNull(request)
    }

    @Test fun `logout during refresh cannot resurrect the old session`() {
        val store = Store(original)
        assertNull(TokenAuthenticator(store, api { store.current = null; success() }).authenticate(null, response()))
        assertNull(store.current)
    }

    @Test fun `public requests cannot start an authentication refresh`() {
        val store = Store(original)
        val authenticator = TokenAuthenticator(store, api { error("must not refresh") })
        assertNull(authenticator.authenticate(null, response(header = false)))
        assertNull(authenticator.authenticate(null, response(tokens = null)))
    }
}
