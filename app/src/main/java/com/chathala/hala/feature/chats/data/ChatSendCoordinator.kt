package com.chathala.hala.feature.chats.data

import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface OutgoingAttachments {
    suspend fun prepare(entry: OutgoingMessage): OutgoingMessage
    suspend fun release(entry: OutgoingMessage)
}

/** Sends only on an explicit user action. Never automatically retries an uncertain delivery. */
class ChatSendCoordinator(
    private val storage: ChatComposerStorage,
    private val tokens: TokenStorage,
    private val scope: CoroutineScope,
    private val attachments: OutgoingAttachments,
    private val interruptedMessage: () -> String,
    private val attachmentError: () -> String,
    private val send: suspend (String, OutgoingMessage) -> OutgoingSendResult
) {
    private val _localFailures = MutableSharedFlow<OutgoingLocalFailure>(extraBufferCapacity = 8)
    val localFailures = _localFailures.asSharedFlow()

    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private fun key(sessionId: String, localId: String) = "$sessionId:$localId"

    fun submit(sessionId: String, conversationId: String, entry: OutgoingMessage, draft: ChatDraft? = null, retrying: Boolean = false): Job? {
        val key = key(sessionId, entry.localId)
        if (!inFlight.add(key)) return null
        val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var prepared = entry.copy(status = OutgoingStatus.PREPARING, failure = null, errorCode = null, promo = null, locked = null)
            try {
                if (!storage.put(sessionId, conversationId, prepared, draft, requireFailed = retrying)) return@launch
                try {
                    prepared = tokens.withSession(sessionId) { attachments.prepare(prepared) } ?: return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    storage.put(sessionId, conversationId, prepared.copy(
                        status = OutgoingStatus.REJECTED, retryAllowed = false,
                        failure = attachmentError(), deliveryUncertain = false
                    ))
                    return@launch
                }
                prepared = prepared.copy(status = OutgoingStatus.SENDING)
                if (!storage.put(sessionId, conversationId, prepared)) return@launch
                val outcome = send(sessionId, prepared)
                val result = outcome.result
                val completed = when (result) {
                    is NetworkResult.Success -> prepared.copy(
                        message = result.data, status = OutgoingStatus.SENT,
                        failure = null, deliveryUncertain = false, promo = outcome.promo
                    )
                    is NetworkResult.Error -> prepared.copy(
                        status = if (result.permitsMessageRetry()) OutgoingStatus.FAILED else OutgoingStatus.REJECTED,
                        failure = result.message,
                        retryAllowed = result.permitsMessageRetry(),
                        deliveryUncertain = result.hasUncertainDelivery(),
                        errorCode = result.code, promo = outcome.promo, locked = outcome.locked
                    )
                }
                val saved = storage.put(sessionId, conversationId, completed)
                if (saved && completed.status == OutgoingStatus.SENT) {
                    try { attachments.release(prepared) } catch (e: CancellationException) { throw e } catch (_: Exception) { /* cleaned on logout */ }
                }
            } catch (e: CancellationException) {
                // A process interruption is recovered as an uncertain failure on next open.
                throw e
            } catch (_: Exception) {
                val failed = prepared.copy(status = OutgoingStatus.FAILED, retryAllowed = true,
                    deliveryUncertain = true, failure = interruptedMessage())
                try {
                    storage.put(sessionId, conversationId, failed)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Report an unavailable disk without crashing the application scope.
                    _localFailures.tryEmit(OutgoingLocalFailure(sessionId, conversationId,
                        failed.copy(status = OutgoingStatus.REJECTED, retryAllowed = false)))
                }
            } finally {
                inFlight.remove(key)
                jobs.remove(key)
            }
        }
        jobs[key] = job
        job.invokeOnCompletion { jobs.remove(key, job) }
        return job
    }

    suspend fun restore(sessionId: String, conversationId: String) {
        storage.readOutgoing(sessionId, conversationId).filter { it.busy }.forEach { previous ->
            val key = key(sessionId, previous.localId)
            if (!inFlight.add(key)) return@forEach
            try {
                val entry = storage.readOutgoing(sessionId, conversationId).firstOrNull { it.localId == previous.localId && it.busy }
                    ?: return@forEach
                val available = entry.message.type == "text" || entry.attachmentPath != null
                storage.put(sessionId, conversationId, entry.copy(
                    status = if (available) OutgoingStatus.FAILED else OutgoingStatus.REJECTED,
                    retryAllowed = available, deliveryUncertain = entry.status == OutgoingStatus.SENDING,
                    failure = if (available) interruptedMessage() else attachmentError()
                ))
            } finally { inFlight.remove(key) }
        }
    }

    suspend fun retry(sessionId: String, conversationId: String, localId: String, confirmed: Boolean = false): Job? {
        val entry = storage.readOutgoing(sessionId, conversationId).firstOrNull { it.localId == localId } ?: return null
        if (entry.status != OutgoingStatus.FAILED || !entry.retryAllowed || (entry.deliveryUncertain && !confirmed)) return null
        return submit(sessionId, conversationId, entry, retrying = true)
    }

    suspend fun discard(sessionId: String, conversationId: String, localId: String) {
        val key = key(sessionId, localId)
        if (!inFlight.add(key)) return
        try {
            val entry = storage.readOutgoing(sessionId, conversationId).firstOrNull { it.localId == localId } ?: return
            if (entry.status == OutgoingStatus.SENT || entry.busy) return
            storage.remove(sessionId, conversationId, setOf(localId))
            attachments.release(entry)
        } finally { inFlight.remove(key) }
    }

    suspend fun clearConversation(sessionId: String, conversationId: String) {
        val entries = storage.readOutgoing(sessionId, conversationId)
        entries.forEach { jobs[key(sessionId, it.localId)]?.cancelAndJoin() }
        entries.forEach { attachments.release(it) }
        storage.remove(sessionId, conversationId, entries.map { it.localId }.toSet())
        storage.saveDraft(sessionId, conversationId, ChatDraft())
    }

    suspend fun releaseLocal(entry: OutgoingMessage) { attachments.release(entry) }

    fun cancelAll() { jobs.values.forEach { it.cancel() } }
}
