package com.chathala.hala.feature.chats.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.combine
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private val Context.chatsDataStore by preferencesDataStore(name = "hala_chats_cache")

/**
 * كاش محلي للمحادثات والرسائل — لعرض البيانات فوراً قبل اكتمال جلب الشبكة.
 *  - قائمة المحادثات: مفتاح واحد ثابت
 *  - رسائل كل محادثة: مفتاح ديناميكي حسب conversationId (آخر 100 رسالة لتجنّب الانتفاخ)
 */
class ChatsCacheStorage internal constructor(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi,
    private val tokens: TokenStorage
) {
    constructor(context: Context, moshi: Moshi, tokens: TokenStorage) : this(context.chatsDataStore, moshi, tokens)

    private val conversationsType =
        Types.newParameterizedType(List::class.java, Conversation::class.java)
    private val conversationsAdapter: JsonAdapter<List<Conversation>> =
        moshi.adapter(conversationsType)

    private val messagesType =
        Types.newParameterizedType(List::class.java, Message::class.java)
    private val messagesAdapter: JsonAdapter<List<Message>> =
        moshi.adapter(messagesType)

    suspend fun sessionId(): String? = tokens.snapshot()?.sessionId

    private val ownerKey = stringPreferencesKey("session_owner")
    private val conversationsKey = stringPreferencesKey("conversations_json")
    private val pinnedKey = stringSetPreferencesKey("pinned_conversation_ids")
    private fun messagesKey(conversationId: String) =
        stringPreferencesKey("messages_${conversationId}_json")

    // ── Pinned conversations (client-side) ────────────────────────

    val pinnedIds: Flow<Set<String>> =
        dataStore.data.combine(tokens.sessionId) { prefs, session ->
            if (session != null && prefs[ownerKey] == session) prefs[pinnedKey] ?: emptySet() else emptySet()
        }

    suspend fun setPinned(conversationId: String, pinned: Boolean, sessionId: String?) {
        tokens.withSession(sessionId) {
            dataStore.edit { prefs ->
                prefs[ownerKey] = sessionId!!
                val current = prefs[pinnedKey] ?: emptySet()
                prefs[pinnedKey] = if (pinned) current + conversationId else current - conversationId
            }
        }
    }

    // ── Conversations list ────────────────────────────────────────

    suspend fun readConversations(sessionId: String?): List<Conversation>? {
        val prefs = dataStore.data.first()
        if (sessionId == null || prefs[ownerKey] != sessionId || sessionId != this.sessionId()) return null
        val json = prefs[conversationsKey] ?: return null
        return runCatching { conversationsAdapter.fromJson(json) }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    suspend fun saveConversations(items: List<Conversation>, sessionId: String?) {
        val json = conversationsAdapter.toJson(items)
        tokens.withSession(sessionId) {
            dataStore.edit {
                it[ownerKey] = sessionId!!
                it[conversationsKey] = json
            }
        }
    }

    // ── Messages per conversation ─────────────────────────────────

    suspend fun readMessages(conversationId: String, sessionId: String?): List<Message>? {
        val prefs = dataStore.data.first()
        if (sessionId == null || prefs[ownerKey] != sessionId || sessionId != this.sessionId()) return null
        val json = prefs[messagesKey(conversationId)] ?: return null
        return runCatching { messagesAdapter.fromJson(json) }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    suspend fun saveMessages(conversationId: String, items: List<Message>, sessionId: String?) {
        // احتفظ بآخر 100 رسالة فقط لتقليل حجم التخزين
        val persistent = items.filterNot { it.id.startsWith("tmp-") || it.disappearing?.enabled == true }
        val tail = persistent.takeLast(100)
        val json = messagesAdapter.toJson(tail)
        tokens.withSession(sessionId) {
            dataStore.edit {
                it[ownerKey] = sessionId!!
                it[messagesKey(conversationId)] = json
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
