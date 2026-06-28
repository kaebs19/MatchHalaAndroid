package com.chathala.hala.feature.discover.data

import android.content.Context
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
class DiscoverCacheStorage(
    private val context: Context,
    moshi: Moshi
) {
    private val listType = Types.newParameterizedType(List::class.java, DiscoverCard::class.java)
    private val adapter: JsonAdapter<List<DiscoverCard>> = moshi.adapter(listType)

    private val cardsKey = stringPreferencesKey("discover_cards_json")
    private val savedAtKey = longPreferencesKey("discover_saved_at")

    suspend fun read(): Cached? {
        val prefs = context.discoverDataStore.data.first()
        val json = prefs[cardsKey] ?: return null
        val savedAt = prefs[savedAtKey] ?: 0L
        val cards = runCatching { adapter.fromJson(json) }.getOrNull().orEmpty()
        if (cards.isEmpty()) return null
        return Cached(cards = cards, savedAt = savedAt)
    }

    suspend fun save(cards: List<DiscoverCard>) {
        val json = adapter.toJson(cards)
        context.discoverDataStore.edit { prefs ->
            prefs[cardsKey] = json
            prefs[savedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun clear() {
        context.discoverDataStore.edit { it.clear() }
    }

    data class Cached(
        val cards: List<DiscoverCard>,
        val savedAt: Long
    )
}
