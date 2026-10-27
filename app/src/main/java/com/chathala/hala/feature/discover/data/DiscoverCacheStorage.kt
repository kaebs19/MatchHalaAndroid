package com.chathala.hala.feature.discover.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chathala.hala.core.storage.TokenStorage
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.first

private val Context.discoverDataStore by preferencesDataStore(name = "hala_discover_cache")

/**
 * كاش محلي لبطاقات الاكتشاف لعرضها فوراً قبل اكتمال طلب الشبكة.
 * يحتفظ بآخر صفحة أولى تم جلبها مع وسم زمني للتحقق من العمر.
 */
class DiscoverCacheStorage internal constructor(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi,
    private val tokens: TokenStorage
) {
    constructor(context: Context, moshi: Moshi, tokens: TokenStorage) : this(context.discoverDataStore, moshi, tokens)

    private val listType = Types.newParameterizedType(List::class.java, DiscoverCard::class.java)
    private val adapter: JsonAdapter<List<DiscoverCard>> = moshi.adapter(listType)

    suspend fun sessionId(): String? = tokens.snapshot()?.sessionId

    private val ownerKey = stringPreferencesKey("session_owner")
    private val cardsKey = stringPreferencesKey("discover_cards_json")
    private val savedAtKey = longPreferencesKey("discover_saved_at")

    suspend fun read(sessionId: String?): Cached? {
        val prefs = dataStore.data.first()
        if (sessionId == null || prefs[ownerKey] != sessionId || sessionId != this.sessionId()) return null
        val json = prefs[cardsKey] ?: return null
        val savedAt = prefs[savedAtKey] ?: 0L
        val cards = runCatching { adapter.fromJson(json) }.getOrNull().orEmpty()
        if (cards.isEmpty()) return null
        return Cached(cards = cards, savedAt = savedAt)
    }

    suspend fun save(cards: List<DiscoverCard>, sessionId: String?) {
        val json = adapter.toJson(cards)
        tokens.withSession(sessionId) {
            dataStore.edit { prefs ->
                prefs[ownerKey] = sessionId!!
                prefs[cardsKey] = json
                prefs[savedAtKey] = System.currentTimeMillis()
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    data class Cached(
        val cards: List<DiscoverCard>,
        val savedAt: Long
    )
}
