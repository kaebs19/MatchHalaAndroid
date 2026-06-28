package com.chathala.hala.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chathala.hala.feature.user.data.User
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "hala_user")

/**
 * يحفظ المستخدم الحالي كـ JSON في DataStore.
 * - `user`: Flow يصدر المستخدم الحالي أو null
 * - `save(user)`: يستبدل الحالي بالكامل
 * - `clear()`: لحذف عند تسجيل الخروج
 */
class UserStorage(
    private val context: Context,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(User::class.java)
    private val userKey = stringPreferencesKey("user_json")

    val user: Flow<User?> = context.userDataStore.data
        .map { prefs ->
            prefs[userKey]?.let {
                runCatching { adapter.fromJson(it) }.getOrNull()
            }
        }
        .catch { emit(null) }

    suspend fun save(user: User) {
        val json = adapter.toJson(user)
        context.userDataStore.edit { it[userKey] = json }
    }

    suspend fun clear() {
        context.userDataStore.edit { it.clear() }
    }
}
