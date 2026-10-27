package com.chathala.hala.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chathala.hala.core.session.SessionTokens
import com.chathala.hala.core.session.SessionTokenStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

private val Context.tokenDataStore by preferencesDataStore(name = "hala_tokens")

class TokenStorage internal constructor(private val dataStore: DataStore<Preferences>) : SessionTokenStore {
    constructor(context: Context) : this(context.tokenDataStore)
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val sessionIdKey = stringPreferencesKey("session_id")
    private val mutations = Mutex()

    /** Local cleanup only; must not call TokenStorage or perform authenticated requests. */
    var onSessionCleared: suspend () -> Unit = {}

    val token: Flow<String?> = dataStore.data.map { it[accessTokenKey] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[refreshTokenKey] }
    val sessionId: Flow<String?> = dataStore.data.map { tokens(it)?.sessionId }

    private fun tokens(prefs: Preferences): SessionTokens? = prefs[accessTokenKey]?.let {
        SessionTokens(it, prefs[refreshTokenKey], prefs[sessionIdKey] ?: "legacy")
    }

    override suspend fun snapshot(): SessionTokens? = tokens(dataStore.data.first())

    /** A login replaces the whole session, including an absent refresh token. */
    suspend fun save(token: String, refreshToken: String?) = mutations.withLock {
        withContext(NonCancellable) {
            onSessionCleared()
            dataStore.edit {
                it.clear()
                it[accessTokenKey] = token
                refreshToken?.let { refresh -> it[refreshTokenKey] = refresh }
                it[sessionIdKey] = UUID.randomUUID().toString()
            }
            snapshot()!!
        }
    }

    override suspend fun replaceIfCurrent(expected: SessionTokens, accessToken: String, refreshToken: String?): Boolean =
        mutations.withLock {
            var replaced = false
            dataStore.edit {
                if (tokens(it) == expected) {
                    it[accessTokenKey] = accessToken
                    refreshToken?.let { refresh -> it[refreshTokenKey] = refresh }
                    it[sessionIdKey] = expected.sessionId
                    replaced = true
                }
            }
            replaced
        }

    override suspend fun clearIfCurrent(expected: SessionTokens): Boolean = mutations.withLock {
        if (snapshot() != expected) return@withLock false
        clearLocked()
        true
    }

    suspend fun clear() = mutations.withLock { clearLocked() }

    /** User-initiated logout still applies after rotation, but never to a later login. */
    suspend fun endSession(expectedId: String): Boolean = mutations.withLock {
        if (snapshot()?.sessionId != expectedId) return@withLock false
        clearLocked()
        true
    }

    private suspend fun clearLocked() = withContext(NonCancellable) {
        // Clean before publishing null so a new sign-in cannot race old cache removal.
        try {
            onSessionCleared()
        } finally {
            dataStore.edit { it.clear() }
        }
    }

    /** Guards cache writes against logout and account switches. */
    suspend fun <T> withSession(expectedId: String?, block: suspend () -> T): T? = mutations.withLock {
        if (expectedId == null || snapshot()?.sessionId != expectedId) null else block()
    }
}
