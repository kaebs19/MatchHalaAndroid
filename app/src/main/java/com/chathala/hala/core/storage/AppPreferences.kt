package com.chathala.hala.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefs by preferencesDataStore(name = "hala_app_prefs")

/** وضع المظهر المُفضّل للمستخدم. */
enum class AppTheme {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromString(s: String?): AppTheme =
            entries.firstOrNull { it.name == s } ?: SYSTEM
    }
}

/** تخزين تفضيلات التطبيق (ليست خاصة بالمستخدم — تبقى بعد Logout). */
class AppPreferences(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")
    private val recentSearchesKey = stringPreferencesKey("recent_searches")
    private val sensitiveContentKey = booleanPreferencesKey("sensitive_content_enabled")

    val theme: Flow<AppTheme> = context.appPrefs.data.map {
        AppTheme.fromString(it[themeKey])
    }

    suspend fun setTheme(theme: AppTheme) {
        context.appPrefs.edit { it[themeKey] = theme.name }
    }

    // ── عمليات بحث سابقة (آخر 8، الأحدث أولاً) ──

    val recentSearches: Flow<List<String>> = context.appPrefs.data.map {
        it[recentSearchesKey]?.split('\n')?.filter { s -> s.isNotBlank() } ?: emptyList()
    }

    suspend fun addRecentSearch(term: String) {
        val t = term.trim()
        if (t.length < 2) return
        context.appPrefs.edit { prefs ->
            val current = prefs[recentSearchesKey]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(t) + current.filterNot { it.equals(t, ignoreCase = true) }).take(8)
            prefs[recentSearchesKey] = updated.joinToString("\n")
        }
    }

    suspend fun removeRecentSearch(term: String) {
        context.appPrefs.edit { prefs ->
            val current = prefs[recentSearchesKey]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
            prefs[recentSearchesKey] = current.filterNot { it.equals(term, ignoreCase = true) }.joinToString("\n")
        }
    }

    suspend fun clearRecentSearches() {
        context.appPrefs.edit { it.remove(recentSearchesKey) }
    }

    // ── تفضيلات المحتوى الحساس ──

    val sensitiveContentEnabled: Flow<Boolean> = context.appPrefs.data.map {
        it[sensitiveContentKey] ?: false
    }

    suspend fun setSensitiveContentEnabled(enabled: Boolean) {
        context.appPrefs.edit { it[sensitiveContentKey] = enabled }
    }

    // ── محادثات موثوقة (auto-reveal) ──

    private val trustedConversationsKey = stringPreferencesKey("trusted_conversations")

    val trustedConversations: kotlinx.coroutines.flow.Flow<Set<String>> = context.appPrefs.data.map {
        it[trustedConversationsKey]?.split(',')?.filter { s -> s.isNotBlank() }?.toSet() ?: emptySet()
    }

    suspend fun trustConversation(id: String) {
        context.appPrefs.edit { prefs ->
            val current = prefs[trustedConversationsKey]?.split(',')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(id)
            prefs[trustedConversationsKey] = current.joinToString(",")
        }
    }

    suspend fun untrustConversation(id: String) {
        context.appPrefs.edit { prefs ->
            val current = prefs[trustedConversationsKey]?.split(',')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.remove(id)
            prefs[trustedConversationsKey] = current.joinToString(",")
        }
    }

    // ── كاش الكلمات الترويجية الديناميكية ──

    private val promoKeywordsCacheKey = stringPreferencesKey("promo_keywords_cache")

    val promoKeywordsCache: kotlinx.coroutines.flow.Flow<String?> = context.appPrefs.data.map {
        it[promoKeywordsCacheKey]
    }

    suspend fun setPromoKeywordsCache(json: String) {
        context.appPrefs.edit { it[promoKeywordsCacheKey] = json }
    }
}
