package com.chathala.hala.feature.chats.socket

import android.util.Log
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.storage.TokenStorage
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * غلاف موحّد حول Socket.IO client.
 *
 *  - connect(): يجلب access token ويفتح اتصال مع auth.token
 *  - disconnect(): يغلق الاتصال (عند Logout)
 *  - connectionState: StateFlow<Boolean> يراقبه الـ UI
 *  - incoming: SharedFlow<SocketEvent> لجميع الـ events الواردة
 *  - emit(): إرسال events للسيرفر
 *
 * Singleton عبر HalaApp — يُعاد استخدام socket واحد طوال حياة التطبيق.
 */
class HalaSocket(
    private val tokenStorage: TokenStorage,
    private val baseUrl: String = ApiClient.BASE_URL
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private var connectJob: Job? = null
    @Volatile private var connecting = false

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _incoming = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val incoming: SharedFlow<SocketEvent> = _incoming.asSharedFlow()

    /** يفتح الاتصال إذا لم يكن مفتوحاً. Safe للاستدعاء المتكرر. */
    fun connect() {
        // أعد استخدام السوكِت القائم بدل إنشاء واحد جديد (يمنع الاتصالات المكرّرة/التذبذب)
        val existing = socket
        if (existing != null) {
            if (!existing.connected()) existing.connect()
            return
        }
        if (connecting) return
        connecting = true
        connectJob?.cancel()
        connectJob = scope.launch {
            val token = tokenStorage.token.first()
            if (token.isNullOrBlank()) {
                Log.d(TAG, "connect: no token — skip")
                connecting = false
                return@launch
            }
            try {
                val opts = IO.Options().apply {
                    auth = mapOf("token" to token)
                    reconnection = true
                    reconnectionAttempts = Int.MAX_VALUE   // لا تستسلم
                    reconnectionDelay = 1000
                    reconnectionDelayMax = 5000
                    transports = arrayOf("websocket")
                }
                val s = IO.socket(baseUrl, opts)
                attachListeners(s)
                socket = s
                s.connect()
                Log.d(TAG, "connecting…")
            } catch (e: Exception) {
                Log.e(TAG, "connect failed: ${e.message}", e)
            } finally {
                connecting = false
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        connecting = false
        socket?.off()
        socket?.disconnect()
        socket = null
        _connected.value = false
    }

    fun emit(event: String, payload: JSONObject? = null) {
        val s = socket ?: return
        if (payload != null) s.emit(event, payload) else s.emit(event)
    }

    // ── Convenience emitters ──────────────────────────────────────

    fun joinConversation(conversationId: String) {
        emit("join-conversation", JSONObject().put("conversationId", conversationId))
    }

    fun leaveConversation(conversationId: String) {
        emit("leave-conversation", conversationId.let { JSONObject().put("id", it) })
    }

    fun sendTyping(conversationId: String, userName: String?) {
        val payload = JSONObject()
            .put("conversationId", conversationId)
            .put("userName", userName ?: "")
        emit("typing", payload)
    }

    fun sendStopTyping(conversationId: String) {
        emit("stop-typing", JSONObject().put("conversationId", conversationId))
    }

    fun markRead(conversationId: String) {
        emit("mark-read", JSONObject().put("conversationId", conversationId))
    }

    fun markDelivered(messageId: String, conversationId: String) {
        val payload = JSONObject()
            .put("messageId", messageId)
            .put("conversationId", conversationId)
        emit("message-delivered", payload)
    }

    // ── Internals ─────────────────────────────────────────────────

    private fun attachListeners(s: Socket) {
        s.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "connected")
            _connected.value = true
        }
        s.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "disconnected")
            _connected.value = false
        }
        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.w(TAG, "connect_error: ${args.firstOrNull()}")
        }

        // ── أحداث السيرفر → Client ──
        s.on("authenticated") { emit(SocketEvent.Authenticated) }
        s.on("request-fcm-token") { args ->
            val json = args.firstOrNull() as? JSONObject
            emit(
                SocketEvent.RequestFcmToken(
                    reason = json?.optString("reason")?.takeIf { it.isNotBlank() }
                )
            )
        }
        s.on("new-message") { args -> args.firstJson()?.let { emit(SocketEvent.NewMessage(it)) } }
        s.on("user-typing") { args -> args.firstJson()?.let { emit(SocketEvent.UserTyping(it)) } }
        s.on("messages-read") { args -> args.firstJson()?.let { emit(SocketEvent.MessagesRead(it)) } }
        s.on("message-delivered") { args -> args.firstJson()?.let { emit(SocketEvent.MessageDelivered(it)) } }
        s.on("conversation:request") { args -> args.firstJson()?.let { emit(SocketEvent.ConversationRequest(it)) } }
        s.on("conversation-accepted") { args -> args.firstJson()?.let { emit(SocketEvent.ConversationAccepted(it)) } }
        s.on("conversation-rejected") { args -> args.firstJson()?.let { emit(SocketEvent.ConversationRejected(it)) } }
        s.on("chat-mode-changed") { args -> args.firstJson()?.let { emit(SocketEvent.ChatModeChanged(it)) } }
        s.on("users-online") { args -> args.firstJson()?.let { emit(SocketEvent.UsersOnline(it)) } }
        s.on("user:online") { args -> args.firstJson()?.let { emit(SocketEvent.UserOnline(it)) } }
        s.on("user:offline") { args -> args.firstJson()?.let { emit(SocketEvent.UserOffline(it)) } }
        s.on("message-reaction") { args -> args.firstJson()?.let { emit(SocketEvent.MessageReaction(it)) } }
        s.on("message-deleted") { args -> args.firstJson()?.let { emit(SocketEvent.MessageDeleted(it)) } }
        s.on("chat-mode-changed") { args -> args.firstJson()?.let { emit(SocketEvent.ChatModeChanged(it)) } }
        s.on("photo-viewed") { args -> args.firstJson()?.let { emit(SocketEvent.PhotoViewed(it)) } }
        s.on("messaging-restriction-lifted") { args ->
            emit(SocketEvent.RestrictionLifted(args.firstJson() ?: JSONObject()))
        }
        s.on("appeal-message") { args ->
            args.firstJson()?.let { emit(SocketEvent.AppealReply(it)) }
        }
    }

    private fun emit(event: SocketEvent) {
        _incoming.tryEmit(event)
    }

    private fun Array<out Any?>?.firstJson(): JSONObject? =
        this?.firstOrNull() as? JSONObject

    private companion object {
        const val TAG = "HalaSocket"
    }
}
