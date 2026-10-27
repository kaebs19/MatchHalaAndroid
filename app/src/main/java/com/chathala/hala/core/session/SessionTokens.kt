package com.chathala.hala.core.session

/** Session identity survives token rotation, but changes on every sign-in. */
data class SessionTokens(val accessToken: String, val refreshToken: String?, val sessionId: String)

interface SessionTokenStore {
    suspend fun snapshot(): SessionTokens?
    suspend fun replaceIfCurrent(expected: SessionTokens, accessToken: String, refreshToken: String?): Boolean
    suspend fun clearIfCurrent(expected: SessionTokens): Boolean
}
