package com.chathala.hala.feature.chats.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ErrorMessages
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.AppPreferences
import com.chathala.hala.feature.chats.audio.AudioPlayer
import com.chathala.hala.feature.chats.audio.AudioRecorder
import com.chathala.hala.feature.chats.data.ChatsCacheStorage
import com.chathala.hala.feature.chats.data.ExternalPromoBlockedInfo
import com.chathala.hala.feature.chats.data.ConversationsRepository
import com.chathala.hala.feature.chats.data.Message
import com.chathala.hala.feature.chats.data.PromoKeywordDetector
import com.chathala.hala.feature.reporting.data.ReportReason
import com.chathala.hala.feature.blocking.data.BlockingRepository
import com.chathala.hala.feature.reporting.data.ReportRepository
import com.chathala.hala.feature.chats.data.MessageReply
import com.chathala.hala.feature.chats.data.MessageReplySender
import com.chathala.hala.feature.chats.data.MessageSender
import com.chathala.hala.feature.chats.data.MessagesRepository
import com.chathala.hala.feature.chats.data.MessagingRestrictionInfo
import com.chathala.hala.feature.chats.data.Reaction
import com.chathala.hala.feature.chats.socket.HalaSocket
import com.chathala.hala.feature.chats.socket.SocketEvent
import com.chathala.hala.feature.user.data.User
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class RevealedSensitiveEntry(val content: String, val expiresAt: Long)

data class ChatUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<Message> = emptyList(),
    val sending: Boolean = false,
    val input: String = "",
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val typingUser: String? = null,
    val socketConnected: Boolean = false,
    val recording: Boolean = false,
    val recordingSeconds: Int = 0,
    val uploadingMedia: Boolean = false,
    val replyingTo: Message? = null,
    val reactionTarget: Message? = null,
    val muteWorking: Boolean = false,
    val chatMode: String = "snap",    // snap | 24h | keep
    val forwardTarget: Message? = null,
    val pendingImageUri: android.net.Uri? = null,
    // مصدر الصورة قيد المعاينة: معرض أو كاميرا
    val pendingImageSource: com.chathala.hala.feature.chats.ui.chat.components.ImageSource =
        com.chathala.hala.feature.chats.ui.chat.components.ImageSource.GALLERY,
    // تتبع: الصور المؤقتة التي شاهدها المستخدم الحالي (messageId → expiresAtMs)
    val viewedDisappearing: Map<String, Long> = emptyMap(),
    // الطرف الآخر (للعنوان والأفاتار)
    val otherUserId: String? = null,
    val otherUserName: String? = null,
    val otherUserAvatar: String? = null,
    val otherUserOnline: Boolean = false,
    val otherUserVerified: Boolean = false,
    val reporting: Boolean = false,
    val deleting: Boolean = false,
    val reopening: Boolean = false,
    val conversationDeleted: Boolean = false,
    val blocking: Boolean = false,
    val blocked: Boolean = false,
    // حالة المحادثة (pending/accepted/rejected/expired) — لمنع الردّ قبل القبول
    val conversationStatus: String? = null,
    val isCreator: Boolean = false,
    // Pagination
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val loadingOlder: Boolean = false,
    // عدّاد الرسائل الجديدة الواردة أثناء تصفّح المستخدم لأعلى (للزر العائم)
    val newIncomingCount: Int = 0,
    // معرّف الرسالة التي يجب التمرير إليها (Reply jump أو unread divider)
    val scrollToMessageId: String? = null,
    // أول رسالة غير مقروءة (لفاصل "الرسائل غير المقروءة")
    val firstUnreadId: String? = null,
    // الرسائل الحساسة المكشوفة (messageId → RevealedSensitiveEntry)
    val revealedSensitive: Map<String, RevealedSensitiveEntry> = emptyMap(),
    // حوار تحذير الترويج الخارجي
    val externalPromoDialog: ExternalPromoBlockedInfo? = null,
    // محادثات موثوقة (auto-reveal)
    val isTrustedConversation: Boolean = false,
    // تحذير استباقي عند الكتابة
    val promoWarningCategory: String? = null,
    // تقييد المراسلة بسبب نشر حسابات خارجية (بانر أعلى المحادثة)
    val messagingRestriction: MessagingRestrictionInfo? = null,
    // إرسال طلب المراجعة للمشرف قيد التنفيذ
    val reviewSubmitting: Boolean = false,
    // أُرسل طلب المراجعة في هذه الجلسة (لعرض "قيد المراجعة")
    val reviewRequested: Boolean = false
) {
    val canSend: Boolean
        get() = conversationStatus == null || conversationStatus == "accepted"

    val hasMore: Boolean get() = currentPage < totalPages
}

class ChatViewModel(
    private val conversationId: String,
    private val messagesRepo: MessagesRepository,
    private val conversationsRepo: ConversationsRepository,
    private val userRepo: UserRepository,
    private val socket: HalaSocket,
    private val cache: ChatsCacheStorage,
    private val reportRepo: ReportRepository,
    private val blockingRepo: BlockingRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    private var typingEmitJob: Job? = null
    private var typingClearJob: Job? = null
    private var recordTimerJob: Job? = null
    // مهام حذف الصور المؤقتة بعد انتهاء وقتها (messageId → Job)
    private val disappearingRemovalJobs = mutableMapOf<String, Job>()
    private val recorder = AudioRecorder()
    val audioPlayer = AudioPlayer()

    init {
        viewModelScope.launch {
            val u: User? = userRepo.currentUser.first()
            _state.update { it.copy(currentUserId = u?.id, currentUserName = u?.name) }
        }

        socket.connected
            .onEach { c -> _state.update { it.copy(socketConnected = c) } }
            .launchIn(viewModelScope)

        socket.incoming
            .onEach(::onSocketEvent)
            .launchIn(viewModelScope)

        // دخول غرفة المحادثة
        socket.joinConversation(conversationId)

        loadMessages()

        // تعليم كمقروء على السيرفر + Socket
        viewModelScope.launch {
            conversationsRepo.markRead(conversationId)
            socket.markRead(conversationId)
        }

        // جلب وضع المحادثة
        viewModelScope.launch {
            val r = conversationsRepo.fetchChatMode(conversationId)
            if (r is NetworkResult.Success) {
                _state.update { it.copy(chatMode = r.data) }
            }
        }

        // تحديد الطرف الآخر من قائمة المحادثات (من كاش أو شبكة)
        viewModelScope.launch { resolveOtherUser() }

        // تحقق إذا كانت المحادثة موثوقة
        viewModelScope.launch {
            appPreferences.trustedConversations.collect { trusted ->
                _state.update { it.copy(isTrustedConversation = conversationId in trusted) }
            }
        }

        // جلب الكلمات الترويجية الديناميكية من الخادم
        viewModelScope.launch {
            val r = messagesRepo.fetchPromoKeywords()
            if (r is NetworkResult.Success) {
                com.chathala.hala.feature.chats.data.PromoKeywordDetector.updateFromServer(r.data)
            }
        }
    }

    private suspend fun resolveOtherUser() {
        val selfId = userRepo.currentUser.first()?.id
        // جرّب من الكاش أولاً (فوري)
        val cached = runCatching { cache.readConversations() }.getOrNull()
        applyOtherFrom(cached, selfId)
        // ثم من الشبكة لضمان التحديث
        val r = conversationsRepo.fetchConversations()
        if (r is NetworkResult.Success) {
            applyOtherFrom(r.data.conversations, selfId)
        }
    }

    private fun applyOtherFrom(
        conversations: List<com.chathala.hala.feature.chats.data.Conversation>?,
        selfId: String?
    ) {
        val conv = conversations?.firstOrNull { it.id == conversationId } ?: return
        val other = conv.participants.firstOrNull { it.id != selfId } ?: return
        _state.update {
            it.copy(
                otherUserId = other.id,
                otherUserName = other.name,
                otherUserAvatar = other.profileImage,
                otherUserOnline = other.isOnline == true,
                otherUserVerified = other.verification?.isVerified == true,
                conversationStatus = conv.status,
                isCreator = conv.creator == selfId
            )
        }
    }

    // ── Report ────────────────────────────────────────────────────
    fun reportOtherUser(reason: ReportReason, description: String?) {
        val uid = _state.value.otherUserId ?: return
        if (_state.value.reporting) return
        _state.update { it.copy(reporting = true) }
        viewModelScope.launch {
            val r = reportRepo.reportUser(uid, reason, description)
            _state.update { it.copy(reporting = false) }
            when (r) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Block ─────────────────────────────────────────────────────
    fun blockOtherUser() {
        val uid = _state.value.otherUserId ?: return
        if (_state.value.blocking || _state.value.blocked) return
        _state.update { it.copy(blocking = true) }
        viewModelScope.launch {
            val r = blockingRepo.block(uid)
            _state.update { it.copy(blocking = false) }
            when (r) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(blocked = true) }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Delete conversation ───────────────────────────────────────
    fun deleteConversation() {
        if (_state.value.deleting) return
        _state.update { it.copy(deleting = true) }
        viewModelScope.launch {
            when (val r = conversationsRepo.deleteConversation(conversationId)) {
                is NetworkResult.Success -> {
                    runCatching { cache.saveMessages(conversationId, emptyList()) }
                    _state.update { it.copy(deleting = false, conversationDeleted = true) }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(deleting = false) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    /** إنهاء/إلغاء المحادثة للطرفين — تبقى الرسائل، ويتطلب طلباً جديداً للاستئناف. */
    fun cancelConversation() {
        if (_state.value.deleting) return
        _state.update { it.copy(deleting = true) }
        viewModelScope.launch {
            when (val r = conversationsRepo.cancelConversation(conversationId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(deleting = false, conversationStatus = "cancelled") }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(deleting = false) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    /** استئناف محادثة منتهية بإرسال طلب جديد للطرف الآخر. */
    fun reopenConversation() {
        if (_state.value.reopening) return
        val targetId = _state.value.otherUserId ?: return
        _state.update { it.copy(reopening = true) }
        viewModelScope.launch {
            when (val r = conversationsRepo.requestConversation(targetId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(reopening = false, conversationStatus = "pending", isCreator = true) }
                    _message.tryEmit("تم إرسال طلب محادثة جديد")
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(reopening = false) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    private fun onSocketEvent(evt: SocketEvent) {
        when (evt) {
            is SocketEvent.NewMessage -> {
                val json = evt.json
                val messageJson = json.optJSONObject("message") ?: json
                val convId = messageJson.optString("conversation")
                if (convId != conversationId) return
                val msg = parseMessage(messageJson) ?: return
                val selfId = _state.value.currentUserId
                if (msg.sender?.id == selfId) {
                    // رسالتي — تجاهل (أضفناها optimistically بالفعل)
                    return
                }
                _state.update {
                    if (it.messages.any { m -> m.id == msg.id }) it
                    else it.copy(
                        messages = it.messages + msg,
                        newIncomingCount = it.newIncomingCount + 1
                    )
                }
                // علّم مقروء
                socket.markRead(conversationId)
                viewModelScope.launch {
                    runCatching { conversationsRepo.markRead(conversationId) }
                }
            }
            is SocketEvent.UserTyping -> {
                val json = evt.json
                val convId = json.optString("conversationId")
                if (convId != conversationId) return
                val isTyping = json.optBoolean("isTyping", false)
                val userName = json.optString("userName").takeIf { it.isNotBlank() }
                _state.update { it.copy(typingUser = if (isTyping) userName else null) }
                typingClearJob?.cancel()
                if (isTyping) {
                    typingClearJob = viewModelScope.launch {
                        delay(5_000)
                        _state.update { it.copy(typingUser = null) }
                    }
                }
            }
            is SocketEvent.MessagesRead -> {
                val json = evt.json
                val convId = json.optString("conversationId")
                if (convId != conversationId) return
                val selfId = _state.value.currentUserId ?: return
                _state.update { s ->
                    s.copy(
                        messages = s.messages.map { m ->
                            if (m.sender?.id == selfId && m.isRead != true) m.copy(
                                isRead = true,
                                isDelivered = true,
                                status = "read"
                            ) else m
                        }
                    )
                }
            }
            is SocketEvent.MessageReaction -> {
                val json = evt.json
                val messageId = json.optString("messageId").takeIf { it.isNotBlank() } ?: return
                val reactionsArr = json.optJSONArray("reactions") ?: return
                val parsed = buildList {
                    for (i in 0 until reactionsArr.length()) {
                        val r = reactionsArr.optJSONObject(i) ?: continue
                        add(
                            Reaction(
                                user = r.optString("user").takeIf { it.isNotBlank() },
                                emoji = r.optString("emoji"),
                                createdAt = r.optString("createdAt").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                applyReactions(messageId, parsed)
            }
            is SocketEvent.MessageDeleted -> {
                val messageId = evt.json.optString("messageId").takeIf { it.isNotBlank() } ?: return
                _state.update { s ->
                    s.copy(
                        messages = s.messages.map {
                            if (it.id == messageId) it.copy(isDeleted = true, content = "", mediaUrl = null)
                            else it
                        }
                    )
                }
            }
            is SocketEvent.ChatModeChanged -> {
                val convId = evt.json.optString("conversationId")
                if (convId != conversationId) return
                val mode = evt.json.optString("chatMode").takeIf { it.isNotBlank() } ?: return
                _state.update { it.copy(chatMode = mode) }
            }
            is SocketEvent.ConversationCancelled -> {
                // الطرف الآخر أنهى المحادثة → اقفل الإرسال فوراً
                val convId = evt.json.optString("conversationId")
                if (convId != conversationId) return
                _state.update { it.copy(conversationStatus = "cancelled") }
                _message.tryEmit("أنهى الطرف الآخر المحادثة")
            }
            is SocketEvent.PhotoViewed -> {
                // الطرف الآخر شاهد صورتي المؤقتة → ابدأ مؤقّت الحذف عند المرسِل
                val msgId = evt.json.optString("messageId").takeIf { it.isNotBlank() }
                    ?: return
                val msg = _state.value.messages.firstOrNull { it.id == msgId } ?: return
                val duration = msg.disappearing?.duration ?: return
                scheduleDisappearingRemoval(msgId, duration)
            }
            is SocketEvent.RestrictionLifted -> {
                // رُفِع تقييد المراسلة فوراً → ألغِ القفل وأبلغ المستخدم
                if (_state.value.messagingRestriction != null) {
                    _state.update { it.copy(messagingRestriction = null) }
                    _message.tryEmit("✅ تم رفع تقييد المراسلة — يمكنك الإرسال الآن")
                }
            }
            else -> Unit
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            // 1) عرض الرسائل المخزّنة فوراً (بدون شاشة تحميل) إن وُجدت
            val cached = runCatching { cache.readMessages(conversationId) }.getOrNull()
            if (!cached.isNullOrEmpty()) {
                _state.update { it.copy(loading = false, messages = cached, error = null) }
            } else {
                _state.update { it.copy(loading = true, error = null) }
            }
            // 2) ثم اجلب من الشبكة وحدّث (page=1 = أحدث الرسائل)
            when (val r = messagesRepo.fetchMessages(conversationId, page = 1)) {
                is NetworkResult.Success -> {
                    val selfId = _state.value.currentUserId
                    val firstUnread = r.data.messages.firstOrNull { m ->
                        m.sender?.id != null && m.sender.id != selfId && m.isRead != true
                    }?.id
                    _state.update {
                        it.copy(
                            loading = false,
                            messages = r.data.messages,
                            error = null,
                            currentPage = r.data.currentPage,
                            totalPages = r.data.totalPages,
                            firstUnreadId = firstUnread,
                            scrollToMessageId = firstUnread ?: r.data.messages.lastOrNull()?.id,
                            messagingRestriction = r.data.messagingRestriction?.takeIf { mr -> mr.restricted }
                        )
                    }
                    runCatching { cache.saveMessages(conversationId, r.data.messages) }
                    // Auto-reveal للمحادثات الموثوقة
                    if (_state.value.isTrustedConversation) {
                        r.data.messages.filter { m ->
                            m.hasFlaggedContent == true && m.sender?.id != selfId
                        }.forEach { m ->
                            revealSensitiveMessage(m)
                        }
                    }
                }
                is NetworkResult.Error -> {
                    if (_state.value.messages.isEmpty()) {
                        _state.update {
                            it.copy(loading = false, error = ErrorMessages.friendly(r))
                        }
                    } else {
                        _state.update { it.copy(loading = false) }
                        _message.tryEmit(ErrorMessages.friendly(r))
                    }
                }
            }
        }
    }

    /** يجلب الصفحة التالية من الرسائل الأقدم (يُستدعى عند sciroll إلى الأعلى). */
    fun loadOlder() {
        val s = _state.value
        if (s.loadingOlder || !s.hasMore) return
        _state.update { it.copy(loadingOlder = true) }
        viewModelScope.launch {
            when (val r = messagesRepo.fetchMessages(conversationId, page = s.currentPage + 1)) {
                is NetworkResult.Success -> {
                    _state.update { cur ->
                        // ادمج الأقدم في الأعلى مع منع التكرار
                        val existingIds = cur.messages.mapTo(HashSet()) { it.id }
                        val older = r.data.messages.filterNot { it.id in existingIds }
                        cur.copy(
                            messages = older + cur.messages,
                            currentPage = r.data.currentPage,
                            totalPages = r.data.totalPages,
                            loadingOlder = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(loadingOlder = false) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    /** يُمسح وسم التمرير بعد تنفيذه. */
    fun consumeScrollTarget() {
        _state.update { it.copy(scrollToMessageId = null) }
    }

    /** عند ضغط زر "↓ رسائل جديدة" — نُصفّر العدّاد. */
    fun resetNewIncoming() {
        _state.update { it.copy(newIncomingCount = 0) }
    }

    /** يُستدعى من الواجهة عند طلب الانتقال إلى الرسالة المُردود عليها. */
    fun jumpToMessage(messageId: String) {
        _state.update { it.copy(scrollToMessageId = messageId) }
    }

    fun onInputChange(text: String) {
        val category = PromoKeywordDetector.getMatchedCategory(text)
        _state.update { it.copy(input = text, promoWarningCategory = category) }
        // أرسل typing مع throttling خفيف
        typingEmitJob?.cancel()
        typingEmitJob = viewModelScope.launch {
            socket.sendTyping(conversationId, _state.value.currentUserName)
            delay(3_000)
            socket.sendStopTyping(conversationId)
        }
    }

    fun send() {
        val content = _state.value.input.trim()
        if (content.isBlank() || _state.value.sending) return
        // مقيّد بسبب نشر حسابات خارجية → منع الإرسال نهائياً
        _state.value.messagingRestriction?.let { r ->
            _message.tryEmit(
                r.hoursLeft?.let { "المراسلة مقفلة — يمكنك الإرسال بعد $it ساعة" }
                    ?: "المراسلة مقفلة مؤقتاً بسبب نشر حسابات خارجية"
            )
            return
        }
        if (!_state.value.canSend) {
            _message.tryEmit("لا يمكن الرد قبل قبول الطلب")
            return
        }

        // optimistic — أضف رسالة مؤقتة
        val tempId = "tmp-${System.currentTimeMillis()}"
        val selfId = _state.value.currentUserId
        val selfName = _state.value.currentUserName
        val replyTarget = _state.value.replyingTo
        val optimistic = Message(
            id = tempId,
            conversation = conversationId,
            sender = MessageSender(id = selfId, name = selfName),
            content = content,
            type = "text",
            status = "sent",
            isRead = false,
            isDelivered = false,
            createdAt = nowIsoUtc(),
            replyTo = replyTarget?.let {
                MessageReply(
                    id = it.id,
                    content = it.content,
                    type = it.type,
                    sender = MessageReplySender(name = it.sender?.name)
                )
            }
        )
        _state.update {
            it.copy(
                messages = it.messages + optimistic,
                input = "",
                sending = true,
                replyingTo = null
            )
        }
        socket.sendStopTyping(conversationId)

        viewModelScope.launch {
            when (val r = messagesRepo.sendText(conversationId, content, replyTo = replyTarget?.id)) {
                is NetworkResult.Success -> {
                    val (real, promoInfo, lockedInfo) = r.data
                    if (lockedInfo != null) {
                        // لم تُرسَل — احذف الرسالة المؤقتة وأظهر dialog التقييد
                        _state.update { s ->
                            s.copy(
                                messages = s.messages.filterNot { it.id == tempId },
                                sending = false,
                                externalPromoDialog = ExternalPromoBlockedInfo(
                                    title = lockedInfo.title,
                                    message = lockedInfo.message,
                                    serverMessage = lockedInfo.serverMessage,
                                    severity = "locked"
                                )
                            )
                        }
                    } else {
                        _state.update { s ->
                            s.copy(
                                messages = s.messages.map { m ->
                                    if (m.id == tempId) real!! else m
                                },
                                sending = false,
                                externalPromoDialog = promoInfo
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { s ->
                        s.copy(
                            messages = s.messages.filterNot { it.id == tempId },
                            sending = false
                        )
                    }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    // ── Reply flow ────────────────────────────────────────────────
    fun startReply(message: Message) {
        if (message.id.startsWith("tmp-")) return
        _state.update { it.copy(replyingTo = message) }
    }

    fun cancelReply() {
        _state.update { it.copy(replyingTo = null) }
    }

    // ── Reactions ─────────────────────────────────────────────────
    fun openReactionSheet(message: Message) {
        if (message.id.startsWith("tmp-")) return
        _state.update { it.copy(reactionTarget = message) }
    }

    fun dismissReactionSheet() {
        _state.update { it.copy(reactionTarget = null) }
    }

    fun react(message: Message, emoji: String) {
        val uid = _state.value.currentUserId ?: return
        if (message.id.startsWith("tmp-")) return

        // toggle محلياً (optimistic)
        val current = message.reactions ?: emptyList()
        val mineSame = current.firstOrNull { it.user == uid && it.emoji == emoji }
        val newReactions = if (mineSame != null) {
            current.filterNot { it.user == uid && it.emoji == emoji }
        } else {
            current.filterNot { it.user == uid } + Reaction(user = uid, emoji = emoji)
        }
        applyReactions(message.id, newReactions)
        _state.update { it.copy(reactionTarget = null) }

        viewModelScope.launch {
            val r = messagesRepo.react(message.id, emoji)
            if (r is NetworkResult.Success) {
                applyReactions(message.id, r.data)
            } else if (r is NetworkResult.Error) {
                // rollback
                applyReactions(message.id, current)
                _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    private fun applyReactions(messageId: String, reactions: List<Reaction>) {
        _state.update { s ->
            s.copy(messages = s.messages.map { if (it.id == messageId) it.copy(reactions = reactions) else it })
        }
    }

    // ── Delete ────────────────────────────────────────────────────
    fun deleteMessage(message: Message) {
        if (message.id.startsWith("tmp-")) return
        // optimistic: mark as deleted
        _state.update { s ->
            s.copy(
                messages = s.messages.map {
                    if (it.id == message.id) it.copy(isDeleted = true, content = "", mediaUrl = null)
                    else it
                },
                reactionTarget = null
            )
        }
        viewModelScope.launch {
            val r = messagesRepo.deleteMessage(message.id)
            if (r is NetworkResult.Error) {
                _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Forward ───────────────────────────────────────────────────
    fun openForward(message: Message) {
        if (message.id.startsWith("tmp-")) return
        _state.update { it.copy(forwardTarget = message, reactionTarget = null) }
    }

    fun dismissForward() {
        _state.update { it.copy(forwardTarget = null) }
    }

    fun confirmForward(targetConversationId: String) {
        val target = _state.value.forwardTarget ?: return
        _state.update { it.copy(forwardTarget = null) }
        viewModelScope.launch {
            val r = messagesRepo.forwardMessage(target.id, targetConversationId)
            when (r) {
                is NetworkResult.Success -> _message.tryEmit("تم إعادة توجيه الرسالة")
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Chat Mode ─────────────────────────────────────────────────
    fun setChatMode(mode: String) {
        if (mode == _state.value.chatMode) return
        val previous = _state.value.chatMode
        _state.update { it.copy(chatMode = mode) }
        viewModelScope.launch {
            val r = conversationsRepo.setChatMode(conversationId, mode)
            if (r is NetworkResult.Error) {
                _state.update { it.copy(chatMode = previous) }
                _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Disappearing Photo ────────────────────────────────────────
    fun previewImage(
        uri: android.net.Uri,
        source: com.chathala.hala.feature.chats.ui.chat.components.ImageSource =
            com.chathala.hala.feature.chats.ui.chat.components.ImageSource.GALLERY
    ) {
        if (isMessagingLocked()) return
        _state.update { it.copy(pendingImageUri = uri, pendingImageSource = source) }
    }

    /** مقيّد بسبب نشر حسابات خارجية — يمنع كل أشكال الإرسال ويُظهر تنبيهاً. */
    private fun isMessagingLocked(): Boolean {
        val r = _state.value.messagingRestriction ?: return false
        _message.tryEmit(
            r.hoursLeft?.let { "المراسلة مقفلة — يمكنك الإرسال بعد $it ساعة" }
                ?: "المراسلة مقفلة مؤقتاً بسبب نشر حسابات خارجية"
        )
        return true
    }

    /** طلب مراجعة تقييد المراسلة من المشرف. */
    fun requestMessagingReview(reason: String) {
        if (_state.value.reviewSubmitting) return
        _state.update { it.copy(reviewSubmitting = true) }
        viewModelScope.launch {
            when (val r = messagesRepo.requestMessagingReview(reason)) {
                is NetworkResult.Success -> {
                    // علّم محلياً أن الطلب أُرسل ليظهر "قيد المراجعة"
                    _state.update { it.copy(reviewSubmitting = false, reviewRequested = true) }
                    _message.tryEmit(r.data)
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(reviewSubmitting = false) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun dismissImagePreview() {
        _state.update { it.copy(pendingImageUri = null) }
    }

    fun sendPendingImage(context: Context, durationSeconds: Int? = null) {
        val uri = _state.value.pendingImageUri ?: return
        val source = if (_state.value.pendingImageSource ==
            com.chathala.hala.feature.chats.ui.chat.components.ImageSource.CAMERA
        ) "camera" else "gallery"
        _state.update { it.copy(pendingImageUri = null) }
        if (durationSeconds != null && durationSeconds > 0) {
            sendDisappearingImage(context, uri, durationSeconds, source)
        } else {
            sendImage(context, uri, source)
        }
    }

    private fun sendDisappearingImage(
        context: Context,
        uri: android.net.Uri,
        duration: Int,
        imageSource: String
    ) {
        if (_state.value.uploadingMedia) return
        val tempId = "tmp-${System.currentTimeMillis()}"
        val selfId = _state.value.currentUserId
        val selfName = _state.value.currentUserName
        val optimistic = Message(
            id = tempId,
            conversation = conversationId,
            sender = MessageSender(id = selfId, name = selfName),
            type = "image",
            mediaUrl = uri.toString(),
            status = "sent",
            isRead = false,
            isDelivered = false,
            createdAt = nowIsoUtc(),
            imageSource = imageSource,
            disappearing = com.chathala.hala.feature.chats.data.DisappearingInfo(
                enabled = true,
                duration = duration
            )
        )
        _state.update {
            it.copy(messages = it.messages + optimistic, uploadingMedia = true)
        }
        viewModelScope.launch {
            when (val r = messagesRepo.sendDisappearingImage(context, conversationId, uri, duration, imageSource)) {
                is NetworkResult.Success -> _state.update { s ->
                    s.copy(
                        messages = s.messages.map { if (it.id == tempId) r.data else it },
                        uploadingMedia = false
                    )
                }
                is NetworkResult.Error -> {
                    _state.update { s ->
                        s.copy(
                            messages = s.messages.filterNot { it.id == tempId },
                            uploadingMedia = false
                        )
                    }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    fun viewDisappearingPhoto(message: Message) {
        val duration = message.disappearing?.duration ?: return
        val expiresAt = System.currentTimeMillis() + duration * 1000L
        _state.update { it.copy(viewedDisappearing = it.viewedDisappearing + (message.id to expiresAt)) }
        viewModelScope.launch {
            messagesRepo.viewDisappearingPhoto(message.id)
        }
        // احذف الصورة عند المستقبِل بعد انتهاء العدّاد
        scheduleDisappearingRemoval(message.id, duration)
    }

    /**
     * يحذف الصورة المؤقتة من المحادثة بعد [durationSeconds] ثانية.
     * يُضاف +600ms ليكتمل تأثير التلاشي قبل الإزالة الفعلية.
     * يُستخدم لكلا الطرفين (المستقبِل عند المشاهدة، والمرسِل عند وصول photo-viewed).
     */
    private fun scheduleDisappearingRemoval(messageId: String, durationSeconds: Int) {
        if (disappearingRemovalJobs.containsKey(messageId)) return
        disappearingRemovalJobs[messageId] = viewModelScope.launch {
            delay(durationSeconds * 1000L + 600L)
            _state.update { s ->
                s.copy(
                    messages = s.messages.filterNot { it.id == messageId },
                    viewedDisappearing = s.viewedDisappearing - messageId
                )
            }
            disappearingRemovalJobs.remove(messageId)
        }
    }

    // ── External Promo Dialog ─────────────────────────────────────

    fun showExternalPromoInfo(message: Message) {
        val categories = message.externalPromoCategories ?: listOf("external_promo")
        _state.update {
            it.copy(
                externalPromoDialog = ExternalPromoBlockedInfo(
                    title = "تم حجب رسالتك",
                    message = "تم التعرف تلقائياً على مشاركة حساب خارجي. سياسة المنصة تمنع ذلك، وتكرار مشاركة حسابات أو أرقام يقيّد حسابك آلياً.",
                    categories = categories,
                    severity = "repeated"
                )
            )
        }
    }

    fun dismissExternalPromoDialog() {
        _state.update { it.copy(externalPromoDialog = null) }
    }

    // ── Sensitive Content Reveal ──────────────────────────────────

    fun revealSensitiveMessage(message: Message) {
        if (_state.value.revealedSensitive.containsKey(message.id)) return
        viewModelScope.launch {
            when (val result = messagesRepo.revealSensitiveContent(message.id)) {
                is NetworkResult.Success -> {
                    val entry = RevealedSensitiveEntry(
                        content = result.data,
                        expiresAt = System.currentTimeMillis() + 30_000L
                    )
                    _state.update { it.copy(revealedSensitive = it.revealedSensitive + (message.id to entry)) }
                    // أزل الكشف بعد 30 ثانية تلقائياً
                    launch {
                        delay(30_000L)
                        dismissReveal(message.id)
                    }
                }
                is NetworkResult.Error -> {
                    val code = result.message
                    when {
                        code?.contains("FEATURE_DISABLED") == true ->
                            _message.tryEmit("الميزة غير متاحة حالياً")
                        code?.contains("USER_SETTING_DISABLED") == true ->
                            _message.tryEmit("فعّل عرض المحتوى الحساس من الإعدادات أولاً")
                        code?.contains("AGE_RESTRICTED") == true ->
                            _message.tryEmit("هذا الخيار للبالغين فقط (+18)")
                        else -> _message.tryEmit(ErrorMessages.friendly(result))
                    }
                }
            }
        }
    }

    fun dismissReveal(messageId: String) {
        _state.update { it.copy(revealedSensitive = it.revealedSensitive - messageId) }
    }

    fun toggleTrustConversation() {
        viewModelScope.launch {
            val willTrust = !_state.value.isTrustedConversation
            if (willTrust) {
                appPreferences.trustConversation(conversationId)
                // كشف فوري للمحتوى الحساس الموجود حالياً (بدل انتظار إعادة التحميل)
                val selfId = _state.value.currentUserId
                _state.value.messages.filter { m ->
                    m.hasFlaggedContent == true && m.sender?.id != selfId
                }.forEach { m -> revealSensitiveMessage(m) }
                _message.tryEmit("تم تفعيل الثقة — يُكشف المحتوى الحساس في هذه المحادثة تلقائياً")
            } else {
                appPreferences.untrustConversation(conversationId)
                _message.tryEmit("تم إلغاء الثقة بهذه المحادثة")
            }
            // state يتحدث تلقائياً عبر الـ Flow collector في init
        }
    }

    fun appealMessageBlock(message: Message, reason: String) {
        viewModelScope.launch {
            when (val r = messagesRepo.appealBlock(message.id, reason)) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Mute ──────────────────────────────────────────────────────
    fun toggleMute(muted: Boolean, mutedUntilIso: String? = null) {
        if (_state.value.muteWorking) return
        _state.update { it.copy(muteWorking = true) }
        viewModelScope.launch {
            val r = conversationsRepo.setMute(conversationId, muted, mutedUntilIso)
            _state.update { it.copy(muteWorking = false) }
            when (r) {
                is NetworkResult.Success -> {
                    _message.tryEmit(if (r.data) "تم كتم المحادثة" else "تم إلغاء الكتم")
                    // حدّث بيانات المستخدم لتنعكس mute indicator فوراً في قائمة المحادثات
                    runCatching { userRepo.refresh() }
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Image ─────────────────────────────────────────────────────
    fun sendImage(context: Context, uri: Uri, imageSource: String = "gallery") {
        if (_state.value.uploadingMedia) return
        val tempId = "tmp-${System.currentTimeMillis()}"
        val selfId = _state.value.currentUserId
        val selfName = _state.value.currentUserName
        val optimistic = Message(
            id = tempId,
            conversation = conversationId,
            sender = MessageSender(id = selfId, name = selfName),
            type = "image",
            mediaUrl = uri.toString(),   // محلي للعرض المؤقت
            status = "sent",
            isRead = false,
            isDelivered = false,
            createdAt = nowIsoUtc(),
            imageSource = imageSource
        )
        _state.update {
            it.copy(messages = it.messages + optimistic, uploadingMedia = true)
        }
        viewModelScope.launch {
            when (val r = messagesRepo.sendImage(context, conversationId, uri, imageSource = imageSource)) {
                is NetworkResult.Success -> _state.update { s ->
                    s.copy(
                        messages = s.messages.map { if (it.id == tempId) r.data else it },
                        uploadingMedia = false
                    )
                }
                is NetworkResult.Error -> {
                    _state.update { s ->
                        s.copy(
                            messages = s.messages.filterNot { it.id == tempId },
                            uploadingMedia = false
                        )
                    }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    // ── Audio recording ───────────────────────────────────────────
    fun startRecording(context: Context) {
        if (_state.value.recording) return
        if (isMessagingLocked()) return
        val r = recorder.start(context)
        r.fold(
            onSuccess = {
                _state.update { it.copy(recording = true, recordingSeconds = 0) }
                recordTimerJob?.cancel()
                recordTimerJob = viewModelScope.launch {
                    var s = 0
                    while (true) {
                        delay(1000)
                        s += 1
                        _state.update { it.copy(recordingSeconds = s) }
                    }
                }
            },
            onFailure = { err ->
                _message.tryEmit(err.message ?: "تعذّر بدء التسجيل")
            }
        )
    }

    fun stopAndSendRecording() {
        if (!_state.value.recording) return
        recordTimerJob?.cancel()
        val res = recorder.stop()
        _state.update { it.copy(recording = false, recordingSeconds = 0) }
        res.fold(
            onSuccess = { rec ->
                val tempId = "tmp-${System.currentTimeMillis()}"
                val selfId = _state.value.currentUserId
                val selfName = _state.value.currentUserName
                val optimistic = Message(
                    id = tempId,
                    conversation = conversationId,
                    sender = MessageSender(id = selfId, name = selfName),
                    type = "audio",
                    mediaUrl = rec.file.absolutePath,
                    audioDuration = rec.durationSeconds,
                    status = "sent",
                    isRead = false,
                    isDelivered = false,
                    createdAt = nowIsoUtc()
                )
                _state.update {
                    it.copy(messages = it.messages + optimistic, uploadingMedia = true)
                }
                viewModelScope.launch {
                    when (val r = messagesRepo.sendAudio(conversationId, rec.file, rec.durationSeconds)) {
                        is NetworkResult.Success -> _state.update { s ->
                            s.copy(
                                messages = s.messages.map { if (it.id == tempId) r.data else it },
                                uploadingMedia = false
                            )
                        }
                        is NetworkResult.Error -> {
                            _state.update { s ->
                                s.copy(
                                    messages = s.messages.filterNot { it.id == tempId },
                                    uploadingMedia = false
                                )
                            }
                            _message.tryEmit(ErrorMessages.friendly(r))
                        }
                    }
                    runCatching { rec.file.delete() }
                }
            },
            onFailure = { err ->
                _message.tryEmit(err.message ?: "فشل التسجيل")
            }
        )
    }

    fun cancelRecording() {
        recordTimerJob?.cancel()
        recorder.cancel()
        _state.update { it.copy(recording = false, recordingSeconds = 0) }
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    override fun onCleared() {
        super.onCleared()
        recordTimerJob?.cancel()
        recorder.cancel()
        audioPlayer.stop()
        socket.leaveConversation(conversationId)
        socket.sendStopTyping(conversationId)
        // احفظ آخر نسخة من الرسائل في الكاش (استبعد الرسائل المؤقتة)
        val snapshot = _state.value.messages.filterNot { it.id.startsWith("tmp-") }
        if (snapshot.isNotEmpty()) {
            kotlinx.coroutines.GlobalScope.launch {
                runCatching { cache.saveMessages(conversationId, snapshot) }
            }
        }
    }

    // محوّل Moshi موحّد — يقرأ كامل حقول الرسالة (disappearing/imageSource/reactions/replyTo…)
    // ويستفيد من المحوّل المرن للحقل sender (كائن أو نصّ).
    private val messageAdapter by lazy {
        com.chathala.hala.core.network.ApiClient.moshi.adapter(Message::class.java)
    }

    /** يحوّل JSON الوارد من السوكت إلى [Message] عبر Moshi (مصدر تحليل واحد للتطبيق). */
    private fun parseMessage(json: JSONObject): Message? =
        runCatching { messageAdapter.fromJson(json.toString()) }
            .getOrNull()
            ?.takeIf { it.id.isNotBlank() }

    private fun nowIsoUtc(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return fmt.format(java.util.Date())
    }

    companion object {
        fun factory(conversationId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                ChatViewModel(
                    conversationId = conversationId,
                    messagesRepo = app.messagesRepository,
                    conversationsRepo = app.conversationsRepository,
                    userRepo = app.userRepository,
                    socket = app.socket,
                    cache = app.chatsCacheStorage,
                    reportRepo = app.reportRepository,
                    blockingRepo = app.blockingRepository,
                    appPreferences = AppPreferences(app)
                )
            }
        }
    }
}
