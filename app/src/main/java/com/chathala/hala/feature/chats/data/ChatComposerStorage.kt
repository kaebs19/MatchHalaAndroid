package com.chathala.hala.feature.chats.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chathala.hala.core.storage.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.composerDataStore by preferencesDataStore(name = "hala_chat_composer")

/** Durable drafts and manual outbox; every read/write belongs to one login session. */
class ChatComposerStorage internal constructor(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi,
    private val tokens: TokenStorage,
    private val scope: CoroutineScope
) {
    constructor(context: Context, moshi: Moshi, tokens: TokenStorage, scope: CoroutineScope) :
        this(context.composerDataStore, moshi, tokens, scope)

    private val draftAdapter = moshi.adapter(ChatDraft::class.java)
    private val outgoingAdapter = moshi.adapter<List<OutgoingMessage>>(
        Types.newParameterizedType(List::class.java, OutgoingMessage::class.java)
    )
    private val _draftSaveFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val draftSaveFailures = _draftSaveFailures.asSharedFlow()

    private val ownerKey = stringPreferencesKey("session_owner")
    private fun draftKey(conversationId: String) = stringPreferencesKey("draft_$conversationId")
    private fun outboxKey(conversationId: String) = stringPreferencesKey("outbox_$conversationId")

    // Start in caller order; the application scope lets the final keystroke survive navigation.
    fun queueDraft(sessionId: String?, conversationId: String, draft: ChatDraft): Job =
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try { saveDraft(sessionId, conversationId, draft) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _draftSaveFailures.tryEmit(Unit) }
        }

    suspend fun saveDraft(sessionId: String?, conversationId: String, draft: ChatDraft) {
        tokens.withSession(sessionId) {
            dataStore.edit { prefs ->
                if (prefs[ownerKey] != sessionId) prefs.clear()
                prefs[ownerKey] = sessionId!!
                if (draft.isEmpty) prefs.remove(draftKey(conversationId))
                else prefs[draftKey(conversationId)] = draftAdapter.toJson(draft)
            }
        }
    }

    suspend fun readDraft(sessionId: String?, conversationId: String): ChatDraft? = tokens.withSession(sessionId) {
        val prefs = dataStore.dataFirst()
        if (prefs[ownerKey] == sessionId) prefs[draftKey(conversationId)]?.let(draftAdapter::fromJson) else null
    }

    fun outgoing(sessionId: String, conversationId: String): Flow<List<OutgoingMessage>> = dataStore.data.map { prefs ->
        if (prefs[ownerKey] == sessionId && tokens.snapshot()?.sessionId == sessionId) readEntries(prefs, conversationId)
        else emptyList()
    }

    suspend fun readOutgoing(sessionId: String?, conversationId: String): List<OutgoingMessage> = tokens.withSession(sessionId) {
        val prefs = dataStore.dataFirst()
        if (prefs[ownerKey] == sessionId) readEntries(prefs, conversationId) else emptyList()
    } ?: emptyList()

    /** Transfer a matching draft into a durable bubble atomically, without deleting newer typing. */
    suspend fun put(sessionId: String, conversationId: String, entry: OutgoingMessage, consumedDraft: ChatDraft? = null, requireFailed: Boolean = false): Boolean =
        tokens.withSession(sessionId) {
            var saved = false
            dataStore.edit { prefs ->
                val entries = if (prefs[ownerKey] == sessionId) readEntries(prefs, conversationId) else emptyList()
                if (requireFailed && entries.none { it.localId == entry.localId && it.status == OutgoingStatus.FAILED && it.retryAllowed }) return@edit
                if (prefs[ownerKey] != sessionId) prefs.clear()
                prefs[ownerKey] = sessionId
                prefs[outboxKey(conversationId)] = outgoingAdapter.toJson(entries.filterNot { it.localId == entry.localId } + entry)
                if (consumedDraft != null && prefs[draftKey(conversationId)]?.let(draftAdapter::fromJson) == consumedDraft) {
                    prefs.remove(draftKey(conversationId))
                }
                saved = true
            }
            saved
        } ?: false

    suspend fun remove(sessionId: String, conversationId: String, localIds: Set<String>) {
        tokens.withSession(sessionId) {
            dataStore.edit { prefs ->
                if (prefs[ownerKey] != sessionId) return@edit
                val remaining = readEntries(prefs, conversationId).filterNot { it.localId in localIds }
                if (remaining.isEmpty()) prefs.remove(outboxKey(conversationId))
                else prefs[outboxKey(conversationId)] = outgoingAdapter.toJson(remaining)
            }
        }
    }

    suspend fun clear() { dataStore.edit { it.clear() } }

    private fun readEntries(prefs: Preferences, conversationId: String): List<OutgoingMessage> =
        prefs[outboxKey(conversationId)]?.let(outgoingAdapter::fromJson).orEmpty()
}

private suspend fun DataStore<Preferences>.dataFirst(): Preferences = data.first()
