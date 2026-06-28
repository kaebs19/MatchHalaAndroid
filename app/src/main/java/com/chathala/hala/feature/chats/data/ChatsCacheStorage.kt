package com.chathala.hala.feature.chats.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.chatsDataStore by preferencesDataStore(name = "hala_chats_cache")

/**
 * كاش محلي للمحادثات والرسائل — لعرض البيانات فوراً قبل اكتمال جلب الشبكة.
 *  - قائمة المحادثات: مفتاح واحد ثابت
 *  - رسائل كل محادثة: مفتاح ديناميكي حسب conversationId (آخر 100 رسالة لتجنّب الانتفاخ)
 */
class ChatsCacheStorage(
    private val context: Context,
    moshi: Moshi
) {
    private val conversationsType =
        Types.newParameterizedType(List::class.java, Conversation::class.java)
    private val conversationsAdapter: JsonAdapter<List<Conversation>> =
        moshi.adapter(conversationsType)

    private val messagesType =
        Types.newParameterizedType(List::class.java, Message::class.java)
    private val messagesAdapter: JsonAdapter<List<Message>> =
        moshi.adapter(messagesType)

    private val conversationsKey = stringPreferencesKey("conversations_json")
    private val pinnedKey = stringSetPreferencesKey("pinned_conversation_ids")
    private fun messagesKey(conversationId: String) =
        stringPreferencesKey("messages_${conversationId}_json")

    // ── Pinned conversations (client-side) ────────────────────────

    val pinnedIds: Flow<Set<String>> =
        context.chatsDataStore.data.map { it[pinnedKey] ?: emptySet() }

    suspend fun setPinned(conversationId: String, pinned: Boolean) {
        context.chatsDataStore.edit { prefs ->
            val current = prefs[pinnedKey] ?: emptySet()
            prefs[pinnedKey] = if (pinned) current + conversationId else current - conversationId
        }
    }

    // ── Conversations list ────────────────────────────────────────

    suspend fun readConversations(): List<Conversation>? {
        val json = context.chatsDataStore.data.first()[conversationsKey] ?: return null
        return runCatching { conversationsAdapter.fromJson(json) }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    suspend fun saveConversations(items: List<Conversation>) {
        val json = conversationsAdapter.toJson(items)
        context.chatsDataStore.edit { it[conversationsKey] = json }
    }

    // ── Messages per conversation ─────────────────────────────────

    suspend fun readMessages(conversationId: String): List<Message>? {
        val json = context.chatsDataStore.data.first()[messagesKey(conversationId)] ?: return null
        return runCatching { messagesAdapter.fromJson(json) }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    suspend fun saveMessages(conversationId: String, items: List<Message>) {
        // احتفظ بآخر 100 رسالة فقط لتقليل حجم التخزين
        val tail = if (items.size > 100) items.takeLast(100) else items
        val json = messagesAdapter.toJson(tail)
        context.chatsDataStore.edit { it[messagesKey(conversationId)] = json }
    }

    suspend fun clear() {
        context.chatsDataStore.edit { it.clear() }
    }
}
