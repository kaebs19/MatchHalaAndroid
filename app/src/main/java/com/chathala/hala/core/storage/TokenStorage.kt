package com.chathala.hala.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore by preferencesDataStore(name = "hala_tokens")

/**
 * يخزّن رموز الجلسة فقط (access + refresh).
 * بيانات المستخدم في [UserStorage] منفصل.
 */
class TokenStorage(private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val token: Flow<String?> = context.tokenDataStore.data.map { it[accessTokenKey] }
    val refreshToken: Flow<String?> = context.tokenDataStore.data.map { it[refreshTokenKey] }

    suspend fun save(token: String, refreshToken: String?) {
        context.tokenDataStore.edit {
            it[accessTokenKey] = token
            if (refreshToken != null) it[refreshTokenKey] = refreshToken
        }
    }

    suspend fun clear() {
        context.tokenDataStore.edit { it.clear() }
    }
}
