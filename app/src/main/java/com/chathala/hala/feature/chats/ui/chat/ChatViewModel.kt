package com.chathala.hala.feature.chats.ui.chat

import com.chathala.hala.feature.chats.data.isVideo
import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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
import com.chathala.hala.feature.chats.data.DELETED_ACCOUNT_NAME
import com.chathala.hala.feature.chats.data.isOtherAccountDeleted
import com.chathala.hala.feature.chats.data.otherParticipant
import com.chathala.hala.feature.chats.data.ExternalPromoBlockedInfo
import com.chathala.hala.feature.chats.data.ConversationsRepository
import com.chathala.hala.feature.chats.data.Message
import com.chathala.hala.feature.chats.data.ChatDraft
import com.chathala.hala.feature.chats.data.ChatComposerStorage
import com.chathala.hala.feature.chats.data.ChatSendCoordinator
import com.chathala.hala.feature.chats.data.OutgoingMessage
import com.chathala.hala.feature.chats.data.OutgoingStatus
import com.chathala.hala.feature.chats.data.mergeCachedMessages
import com.chathala.hala.feature.chats.data.mergeFetchedMessages
import com.chathala.hala.feature.chats.data.messageTimestamp
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
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

data class RevealedSensitiveEntry(val content: String, val expiresAt: Long)

data class ChatUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<Message> = emptyList(),
    val sending: Boolean = false,
    val input: String = "",
    val composerReady: Boolean = false,
    val outgoing: List<OutgoingMessage> = emptyList(),
    val retryConfirmationId: String? = null,
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
    // المحرّر الموحّد: صور (واحدة أو أكثر) أو فيديو قيد التحرير
    val pendingImages: List<android.net.Uri> = emptyList(),
    val pendingVideo: android.net.Uri? = null,
    // فيديو مؤقت قيد المشاهدة (نُزّل كاملاً)
    val playingVideo: PlayingVideo? = null,
    // فيديوهاتي التي شاهدها الطرف الآخر (سوكت photo-viewed)
    val viewedVideos: Set<String> = emptySet(),
    // رفض الحدّ اليومي للفيديو — نصّ الخادم
    val videoLimitMessage: String? = null,
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
    // الطرف الآخر موقوف بالكامل → نمنع إرسال رسائل جديدة
    val otherUserSuspended: Boolean = false,
    // الطرف الآخر حذف حسابه (اختفى من مشاركي المحادثة) → عرض "حساب محذوف" ومنع المراسلة
    val otherUserDeleted: Boolean = false,
    val reporting: Boolean = false,
    val deleting: Boolean = false,
    val reopening: Boolean = false,
    val conversationDeleted: Boolean = false,
    val blocking: Boolean = false,
    /** أنا حظرت الطرف الآخر */
    val blocked: Boolean = false,
    /** الطرف الآخر حظرني — يُفحص عند كل فتح للمحادثة */
    val blockedByThem: Boolean = false,
    /** المشاهدة جارية لكن الصورة لم تُحمَّل بعد (لا يبدأ المؤقّت قبل التحميل) */
    val loadingDisappearing: Set<String> = emptySet(),
    /** صور مؤقتة دُمِّرت على الخادم — تُعرض «انتهت» */
    val expiredDisappearing: Set<String> = emptySet(),
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
    val reviewRequested: Boolean = false,
    // اشتراك مدفوع — يفتح التعديل والحذف
    val isPremium: Boolean = false,
    // الرسالة قيد التعديل حالياً (تفتح نافذة التحرير)
    val editingMessage: Message? = null,
    val editSubmitting: Boolean = false
) {
    val canSend: Boolean
        get() = !blocked && !blockedByThem && !otherUserDeleted &&
            (conversationStatus == null || conversationStatus == "accepted")

    val hasMore: Boolean get() = currentPage < totalPages
}

/** فيديو مؤقت جاهز للعرض: الملف المنزَّل كاملاً وموعد إغلاقه (24h). */
data class PlayingVideo(
    val messageId: String,
    val file: java.io.File,
    val once: Boolean,
    val isMine: Boolean,
    val deadlineMs: Long?
)

class ChatViewModel(
    private val conversationId: String,
    private val messagesRepo: MessagesRepository,
    private val conversationsRepo: ConversationsRepository,
    private val userRepo: UserRepository,
    private val socket: HalaSocket,
    private val cache: ChatsCacheStorage,
    private val reportRepo: ReportRepository,
    private val blockingRepo: BlockingRepository,
    private val appPreferences: AppPreferences,
    private val appContext: Context,
    private val composerStorage: ChatComposerStorage,
    private val sendCoordinator: ChatSendCoordinator
) : ViewModel() {

    private val cacheSession = viewModelScope.async(start = CoroutineStart.UNDISPATCHED) { cache.sessionId() }

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    private var messagesLoadJob: Job? = null
    private var composerLoadJob: Job? = null
    private var composerSessionId: String? = null
    private val stagedOutgoing = mutableMapOf<String, OutgoingMessage>()
    private val handledOutgoing = mutableSetOf<String>()
    private var typingEmitJob: Job? = null
    // هل أعلنّا للطرف الآخر أننا نكتب حالياً؟ (لمنع إرسال typing مع كل حرف)
    private var isTypingNotified = false
    private var typingClearJob: Job? = null
    private var recordTimerJob: Job? = null
    // مهام حذف الصور المؤقتة بعد انتهاء وقتها (messageId → Job)
    private val disappearingRemovalJobs = mutableMapOf<String, Job>()
    private val recorder = AudioRecorder()
    val audioPlayer = AudioPlayer()

    init {
        viewModelScope.launch {
            sendCoordinator.localFailures.collect { failed ->
                if (failed.sessionId == composerSessionId && failed.conversationId == conversationId) {
                    stagedOutgoing[failed.entry.localId] = failed.entry
                    applyOutgoing(_state.value.outgoing.filterNot { it.localId == failed.entry.localId })
                    _message.tryEmit(S.get(R.string.chat_draft_save_failed))
                }
            }
        }
        viewModelScope.launch {
            composerStorage.draftSaveFailures.collect { _message.tryEmit(S.get(R.string.chat_draft_save_failed)) }
        }
        viewModelScope.launch {
            state.filter { !it.loading }.map { it.messages }.distinctUntilChanged().collectLatest { messages ->
                delay(200)
                try {
                    cache.saveMessages(conversationId, messages, cacheSession.await())
                    // Acknowledge local success only after its server bubble is safely cached.
                    val savedIds = messages.map { it.id }.toSet()
                    val completed = _state.value.outgoing.filter { it.status == OutgoingStatus.SENT && it.message.id in savedIds }
                    composerSessionId?.let { session ->
                        if (completed.isNotEmpty()) composerStorage.remove(session, conversationId, completed.map { it.localId }.toSet())
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: java.io.IOException) {
                    // Cache failure must not prevent live messaging.
                }
            }
        }
        // المحادثة معروضة الآن: رسائلها تصل أمام المستخدم مباشرةً، فلا شريط داخلياً لها.
        com.chathala.hala.feature.push.InAppAlerts.setActiveConversation(conversationId)
        viewModelScope.launch {
            userRepo.currentUser.collect { u ->
                _state.update {
                    it.copy(
                        currentUserId = u?.id,
                        currentUserName = u?.name,
                        isPremium = u?.isPremium == true
                    )
                }
            }
        }

        socket.connected
            .onEach { c ->
                _state.update { it.copy(socketConnected = c) }
                // ادخل غرفة المحادثة عند **كل** اتصال: عند فتح الشاشة قبل اكتمال الاتصال
                // كان الانضمام يُهمَل بصمت، فلا تصل أحداث الغرفة (يكتب / رسالة جديدة).
                if (c) {
                    socket.joinConversation(conversationId)
                    socket.markRead(conversationId)
                    if (!_state.value.loading) loadMessages()
                }
            }
            .launchIn(viewModelScope)

        socket.incoming
            .onEach(::onSocketEvent)
            .launchIn(viewModelScope)

        initializeComposer()

        // تعليم كمقروء — يتكفّل به مراقب الاتصال أعلاه عبر السوكِت.
        // HTTP كـ fallback فقط لو السوكِت غير متصل، لضمان حفظ الحالة.
        viewModelScope.launch {
            if (!socket.connected.value) {
                runCatching { conversationsRepo.markRead(conversationId) }
            }
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
        // جرّب من الكاش أولاً (فوري) — القائمة المخزّنة محلياً
        val cached = runCatching { cache.readConversations(cacheSession.await()) }.getOrNull()
            ?.firstOrNull { it.id == conversationId }
        applyOtherFrom(cached, selfId)
        // ثم من الشبكة: نجلب هذه المحادثة فقط (بدل القائمة كاملة) → أسرع بكثير
        val r = conversationsRepo.fetchConversation(conversationId)
        if (r is NetworkResult.Success) {
            applyOtherFrom(r.data, selfId)
        }
        // الحظر متبادل الأثر — نفحص الاتجاهين عند كل فتح للمحادثة، لا عند الحالة المغلقة فقط
        _state.value.otherUserId?.let { refreshBlockStatus(it) }
    }

    /** يجلب حالة الحظر بالاتجاهين ويقفل الإدخال إن وُجد حظر في أي اتجاه. */
    private suspend fun refreshBlockStatus(otherId: String) {
        val r = blockingRepo.checkBlockStatus(otherId)
        if (r is NetworkResult.Success) {
            _state.update {
                it.copy(blocked = r.data.isBlocked, blockedByThem = r.data.blockedByThem)
            }
        }
    }

    private fun applyOtherFrom(
        conv: com.chathala.hala.feature.chats.data.Conversation?,
        selfId: String?
    ) {
        conv ?: return
        val other = conv.otherParticipant(selfId)
        if (other == null) {
            // لا طرف آخر في المحادثة → حسابه محذوف (الخادم يحذف الوثيقة ويُبقي الرسائل)
            if (conv.isOtherAccountDeleted(selfId)) {
                _state.update {
                    it.copy(
                        otherUserDeleted = true,
                        otherUserName = DELETED_ACCOUNT_NAME,
                        otherUserAvatar = null,
                        otherUserOnline = false,
                        otherUserVerified = false,
                        conversationStatus = conv.status,
                        isCreator = conv.creator == selfId
                    )
                }
            }
            return
        }
        _state.update {
            it.copy(
                otherUserDeleted = false,
                otherUserId = other.id,
                otherUserName = other.name,
                otherUserAvatar = other.profileImage,
                otherUserOnline = other.isOnline == true,
                otherUserVerified = other.verification?.isVerified == true,
                otherUserSuspended = other.isSuspendedAccount == true,
                conversationStatus = conv.status,
                isCreator = conv.creator == selfId
            )
        }
    }

    /** الضغط على أفاتار/اسم حساب محذوف — لا ملف شخصي يُفتح. */
    fun notifyOtherAccountDeleted() {
        _message.tryEmit(S.get(R.string.chat_deleted_no_profile))
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
                    composerSessionId?.let { sendCoordinator.clearConversation(it, conversationId) }
                    stagedOutgoing.clear()
                    runCatching { cache.saveMessages(conversationId, emptyList(), cacheSession.await()) }
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
                    _message.tryEmit(S.get(R.string.chat_new_request_sent))
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(reopening = false) }
                    if (!com.chathala.hala.core.network.RestrictionCenter.consumeNewChatPause(r)) {
                        _message.tryEmit(ErrorMessages.friendly(r))
                    }
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
                // أبلغ المرسِل بالتسليم أولاً (سهمان باهتان عنده) ثم بالقراءة
                socket.markDelivered(msg.id, conversationId)
                // علّم مقروء عبر السوكِت فقط — متصل بالتأكيد (وصلتنا رسالة منه للتو)،
                // والخادم يحدّث readBy + status ويُشعِر الطرف الآخر. لا حاجة لطلب HTTP زائد.
                socket.markRead(conversationId)
            }
            is SocketEvent.MessageDelivered -> {
                val json = evt.json
                val convId = json.optString("conversationId")
                if (convId.isNotBlank() && convId != conversationId) return
                val selfId = _state.value.currentUserId ?: return
                val messageId = json.optString("messageId").takeIf { it.isNotBlank() }
                _state.update { s ->
                    s.copy(
                        messages = s.messages.map { m ->
                            val target = messageId == null || m.id == messageId
                            if (target && m.sender?.id == selfId && m.isRead != true &&
                                m.isDelivered != true
                            ) {
                                m.copy(isDelivered = true, status = "delivered")
                            } else m
                        }
                    )
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
            is SocketEvent.MessageEdited -> {
                val json = evt.json
                val convId = json.optString("conversationId")
                if (convId.isNotBlank() && convId != conversationId) return
                val messageId = json.optString("messageId").takeIf { it.isNotBlank() } ?: return
                val newContent = json.optString("content")
                _state.update { st ->
                    st.copy(
                        messages = st.messages.map {
                            if (it.id == messageId)
                                it.copy(content = newContent, isEdited = true)
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
            is SocketEvent.PartnerAccountState -> {
                // حُظر الطرف الآخر أو عاد نشطاً — نعيد جلبه فتتحدّث الترويسة وشريط «غير متاح»
                val uid = evt.json.optString("userId")
                if (uid.isBlank() || uid != _state.value.otherUserId) return
                viewModelScope.launch {
                    val selfId = userRepo.currentUser.first()?.id
                    val r = conversationsRepo.fetchConversation(conversationId)
                    if (r is NetworkResult.Success) applyOtherFrom(r.data, selfId)
                    else if (evt.json.has("suspended")) {
                        _state.update { it.copy(otherUserSuspended = evt.json.optBoolean("suspended")) }
                    }
                }
            }
            is SocketEvent.ConversationBlockState -> {
                // الحظر/إلغاؤه والمحادثة مفتوحة: يُقفل الحقل ويظهر الشريط فوراً (لا ننتظر
                // رفض الخادم أو إعادة الفتح)، ويختفي عند الإلغاء
                val ids = evt.json.optJSONArray("conversationIds") ?: return
                if ((0 until ids.length()).none { ids.optString(it) == conversationId }) return
                val blockState = evt.json.opt("blockState").takeUnless { it == org.json.JSONObject.NULL } as? String
                _state.update {
                    when (blockState) {
                        "iBlocked" -> it.copy(blocked = true)
                        "blockedMe" -> it.copy(blockedByThem = true)
                        else -> it.copy(blocked = false, blockedByThem = false)
                    }
                }
                // الإلغاء قد يكون من طرف واحد فقط — نؤكّد الحالة بالاتجاهين من الخادم
                if (blockState == null) _state.value.otherUserId?.let { viewModelScope.launch { refreshBlockStatus(it) } }
            }
            is SocketEvent.ConversationCancelled -> {
                // الطرف الآخر أنهى المحادثة → اقفل الإرسال فوراً
                val convId = evt.json.optString("conversationId")
                if (convId != conversationId) return
                _state.update { it.copy(conversationStatus = "cancelled") }
                _message.tryEmit(S.get(R.string.chat_other_ended))
            }
            is SocketEvent.PhotoViewed -> {
                // الطرف الآخر شاهد صورتي المؤقتة → ابدأ مؤقّت الحذف عند المرسِل
                val msgId = evt.json.optString("messageId").takeIf { it.isNotBlank() }
                    ?: return
                val msg = _state.value.messages.firstOrNull { it.id == msgId } ?: return
                // ⚠️ للفيديو duration طوله لا مهلته — لا نجدول إخفاءه بها (فيديو 24h كان
                // يختفي بعد ثوانٍ في iOS). الخادم يرسل photo-expired حين يُدمَّر.
                if (msg.isVideo()) {
                    _state.update { it.copy(viewedVideos = it.viewedVideos + msgId) }
                    return
                }
                val duration = msg.disappearing?.duration ?: return
                // أظهر للمرسِل أنها فُتحت وتختفي بعد N ثانية
                _state.update {
                    it.copy(
                        viewedDisappearing = it.viewedDisappearing +
                            (msgId to System.currentTimeMillis() + duration * 1000L)
                    )
                }
                scheduleDisappearingRemoval(msgId, _state.value.viewedDisappearing.getValue(msgId))
            }
            is SocketEvent.PhotoExpired -> {
                // الخادم دمّر الصورة — امسحها من الكاش والواجهة فوراً عند الطرفين
                val msgId = evt.json.optString("messageId").takeIf { it.isNotBlank() } ?: return
                disappearingRemovalJobs.remove(msgId)?.cancel()
                if (_state.value.playingVideo?.messageId == msgId) _state.update { it.copy(playingVideo = null) }
                _state.value.messages.firstOrNull { it.id == msgId }?.mediaUrl?.let(::evictImageCache)
                _state.update { s ->
                    s.copy(
                        messages = s.messages.map {
                            if (it.id == msgId) it.copy(mediaUrl = null) else it
                        },
                        expiredDisappearing = s.expiredDisappearing + msgId,
                        viewedDisappearing = s.viewedDisappearing - msgId,
                        loadingDisappearing = s.loadingDisappearing - msgId
                    )
                }
            }
            is SocketEvent.RestrictionLifted -> {
                // رُفِع تقييد المراسلة فوراً → ألغِ القفل وأبلغ المستخدم
                if (_state.value.messagingRestriction != null) {
                    _state.update { it.copy(messagingRestriction = null) }
                    _message.tryEmit(S.get(R.string.chat_restriction_lifted))
                }
            }
            else -> Unit
        }
    }

    private fun initializeComposer() {
        if (composerLoadJob?.isActive == true || _state.value.composerReady) return
        composerLoadJob = viewModelScope.launch {
            try {
                val session = cacheSession.await() ?: return@launch
                composerSessionId = session
                val draft = composerStorage.readDraft(session, conversationId)
                sendCoordinator.restore(session, conversationId)
                _state.update { it.copy(
                    input = draft?.text.orEmpty(),
                    replyingTo = draft?.reply?.let { reply -> Message(
                        id = reply.id ?: return@let null, content = reply.content, type = reply.type,
                        sender = MessageSender(name = reply.sender?.name)
                    ) },
                    promoWarningCategory = PromoKeywordDetector.getMatchedCategory(draft?.text.orEmpty()),
                    composerReady = true
                ) }
                applyOutgoing(composerStorage.readOutgoing(session, conversationId))
                viewModelScope.launch { composerStorage.outgoing(session, conversationId).collect(::applyOutgoing) }
                loadMessages()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, error = S.get(R.string.chat_local_load_failed)) }
            }
        }
    }

    private fun applyOutgoing(storedEntries: List<OutgoingMessage>) {
        storedEntries.forEach { stagedOutgoing.remove(it.localId) }
        val entries = storedEntries + stagedOutgoing.values
        _state.update { current ->
            val localIds = current.outgoing.map { it.localId }.toSet()
            val existing = current.messages.filterNot { it.id in localIds }.associateBy { it.id }
            val merged = existing.toMutableMap()
            entries.forEach { entry ->
                if (entry.status != OutgoingStatus.SENT || entry.message.id !in merged) {
                    merged[entry.message.id] = entry.displayMessage()
                }
            }
            current.copy(
                outgoing = entries,
                messages = merged.values.sortedBy { messageTimestamp(it.createdAt) ?: 0L },
                sending = entries.any { it.busy && it.message.type == "text" },
                uploadingMedia = entries.any { it.busy && it.message.type != "text" }
            )
        }
        entries.filter { !it.busy && handledOutgoing.add(it.localId + it.status.name) }.forEach { entry ->
            if (entry.promo != null) _state.update { it.copy(externalPromoDialog = entry.promo) }
            entry.locked?.let { locked ->
                _state.update { it.copy(
                    messagingRestriction = MessagingRestrictionInfo(restricted = true, hoursLeft = locked.hoursLeft, lockedUntil = locked.lockedUntil),
                    externalPromoDialog = ExternalPromoBlockedInfo(title = locked.title, message = locked.message, severity = "locked")
                ) }
            }
            when (entry.errorCode) {
                "CONVERSATION_CANCELLED" -> _state.update { it.copy(conversationStatus = "cancelled") }
                "CONVERSATION_INACTIVE" -> _state.update { it.copy(conversationStatus = "expired") }
                "RECIPIENT_SUSPENDED" -> _state.update { it.copy(otherUserSuspended = true) }
                "USER_BLOCKED" -> _state.value.otherUserId?.let { viewModelScope.launch { refreshBlockStatus(it) } }
                "VIDEO_LIMIT_REACHED" -> _state.update { it.copy(videoLimitMessage = entry.failure) }
            }
        }
    }

    private fun currentDraft() = ChatDraft(_state.value.input, _state.value.replyingTo?.let {
        MessageReply(id = it.id, content = it.content, type = it.type, sender = MessageReplySender(it.sender?.name))
    })

    private fun persistDraft() {
        if (_state.value.composerReady) composerStorage.queueDraft(composerSessionId, conversationId, currentDraft())
    }

    fun loadMessages() {
        if (!_state.value.composerReady) { initializeComposer(); return }
        if (messagesLoadJob?.isActive == true) return
        messagesLoadJob = viewModelScope.launch {
            // 1) عرض الرسائل المخزّنة فوراً (بدون شاشة تحميل) إن وُجدت
            val cached = runCatching { cache.readMessages(conversationId, cacheSession.await()) }.getOrNull()
            if (!cached.isNullOrEmpty() && _state.value.loading) {
                _state.update { it.copy(loading = false, messages = mergeCachedMessages(cached, it.messages), error = null) }
            } else if (_state.value.messages.isNotEmpty()) {
                // Keep live messages while reconciling after reconnect.
            } else {
                _state.update { it.copy(loading = true, error = null) }
            }
            // 2) ثم اجلب من الشبكة وحدّث (page=1 = أحدث الرسائل)
            val before = _state.value.messages
            val deliveredBefore = composerStorage.readOutgoing(cacheSession.await(), conversationId)
                .filter { it.status == OutgoingStatus.SENT }.map { it.localId }.toSet()
            when (val r = messagesRepo.fetchMessages(conversationId, page = 1)) {
                is NetworkResult.Success -> {
                    val selfId = _state.value.currentUserId
                    val firstUnread = r.data.messages.firstOrNull { m ->
                        m.sender?.id != null && m.sender.id != selfId && m.isRead != true
                    }?.id
                    _state.update {
                        it.copy(
                            loading = false,
                            messages = mergeFetchedMessages(r.data.messages, before, it.messages),
                            error = null,
                            currentPage = r.data.currentPage,
                            totalPages = r.data.totalPages,
                            firstUnreadId = firstUnread,
                            scrollToMessageId = firstUnread ?: r.data.messages.lastOrNull()?.id,
                            messagingRestriction = r.data.messagingRestriction?.takeIf { mr -> mr.restricted }
                        )
                    }
                    composerSessionId?.let { composerStorage.remove(it, conversationId, deliveredBefore) }
                    restoreDisappearingDeadlines()
                    runCatching { cache.saveMessages(conversationId, _state.value.messages, cacheSession.await()) }
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
                    restoreDisappearingDeadlines()
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
        if (!_state.value.composerReady || _state.value.deleting) return
        _state.update { it.copy(input = text, promoWarningCategory = category) }
        persistDraft()
        // أعلِن «يكتب» مرة واحدة عند بدء الكتابة فقط — لا مع كل حرف —
        // وأعِد ضبط مؤقّت التوقّف بعد 3 ثوانٍ من آخر ضغطة.
        if (!isTypingNotified) {
            isTypingNotified = true
            socket.sendTyping(conversationId, _state.value.currentUserName)
        }
        typingEmitJob?.cancel()
        typingEmitJob = viewModelScope.launch {
            delay(3_000)
            isTypingNotified = false
            socket.sendStopTyping(conversationId)
        }
    }

    /** يوقف إعلان «يكتب» فوراً (بعد الإرسال أو الخروج). */
    private fun stopTypingNow() {
        typingEmitJob?.cancel()
        if (isTypingNotified) {
            isTypingNotified = false
            socket.sendStopTyping(conversationId)
        }
    }

    private fun canSendNow(): Boolean {
        val state = _state.value
        // رفع وسيط/إرسال جارٍ لا يحجز إرسالاً آخر (لكلٍّ فقاعته) — إلا داخل طلب محادثة
        // معلّق حيث المسموح رسالة واحدة
        val pendingRequest = state.conversationStatus == "pending"
        if (!state.composerReady || state.deleting || state.recording) return false
        if (pendingRequest && (state.sending || state.uploadingMedia)) return false
        val error = when {
            state.otherUserDeleted -> S.get(R.string.chat_cannot_message_deleted_acc)
            state.otherUserSuspended -> S.get(R.string.chat_cannot_message_suspended)
            state.messagingRestriction != null -> state.messagingRestriction.hoursLeft?.let { S.get(R.string.chat_locked_send_after, it) }
                ?: S.get(R.string.chat_locked_promo)
            !state.canSend -> S.get(R.string.chat_cannot_reply_before_accept)
            else -> null
        }
        if (error != null) _message.tryEmit(error)
        return error == null
    }

    private fun outgoingMessage(type: String, content: String? = null, mediaUrl: String? = null): Message = Message(
        id = "tmp-${java.util.UUID.randomUUID()}", conversation = conversationId,
        sender = MessageSender(id = _state.value.currentUserId, name = _state.value.currentUserName),
        content = content, type = type, mediaUrl = mediaUrl, status = "sending",
        isRead = false, isDelivered = false, createdAt = nowIsoUtc()
    )

    private fun enqueue(message: Message, consumedDraft: ChatDraft? = null) {
        val session = composerSessionId ?: return
        val entry = OutgoingMessage(localId = message.id, message = message)
        stagedOutgoing[entry.localId] = entry
        _state.update { it.copy(
            messages = it.messages + message,
            outgoing = it.outgoing + entry,
            sending = message.type == "text", uploadingMedia = message.type != "text"
        ) }
        sendCoordinator.submit(session, conversationId, entry, consumedDraft)
    }

    fun send() {
        val draft = currentDraft()
        if (draft.text.isBlank() || !canSendNow()) return
        val message = outgoingMessage("text", content = draft.text.trim()).copy(replyTo = draft.reply)
        _state.update { it.copy(input = "", replyingTo = null, promoWarningCategory = null) }
        stopTypingNow()
        enqueue(message, draft)
    }

    fun retryMessage(localId: String, confirmed: Boolean = false) {
        val entry = _state.value.outgoing.firstOrNull { it.localId == localId } ?: return
        if (entry.status != OutgoingStatus.FAILED || !entry.retryAllowed || !canSendNow()) return
        if (entry.deliveryUncertain && !confirmed) {
            _state.update { it.copy(retryConfirmationId = localId) }
            return
        }
        dismissRetryConfirmation()
        // Update synchronously so a second tap cannot enqueue the same message.
        _state.update { it.copy(sending = entry.message.type == "text", uploadingMedia = entry.message.type != "text") }
        viewModelScope.launch {
            composerSessionId?.let { sendCoordinator.retry(it, conversationId, localId, confirmed) }
        }
    }

    fun dismissRetryConfirmation() { _state.update { it.copy(retryConfirmationId = null) } }

    fun discardFailedMessage(localId: String) {
        val entry = _state.value.outgoing.firstOrNull { it.localId == localId } ?: return
        if (entry.busy || entry.status == OutgoingStatus.SENT) return
        viewModelScope.launch {
            if (stagedOutgoing.remove(localId) != null) {
                sendCoordinator.releaseLocal(entry)
                _state.update { it.copy(outgoing = it.outgoing.filterNot { item -> item.localId == localId }, messages = it.messages.filterNot { item -> item.id == localId }) }
            } else {
                composerSessionId?.let { sendCoordinator.discard(it, conversationId, localId) }
            }
        }
    }

    // ── Reply flow ────────────────────────────────────────────────
    fun startReply(message: Message) {
        if (message.id.startsWith("tmp-")) return
        _state.update { it.copy(replyingTo = message) }
        persistDraft()
    }

    fun cancelReply() {
        _state.update { it.copy(replyingTo = null) }
        persistDraft()
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

    // ── Edit (بريميوم) ────────────────────────────────────────────

    /** نافذة التعديل المسموحة — تطابق الخادم (15 دقيقة من الإرسال). */
    private val editWindowMs = 15 * 60 * 1000L

    /** هل يمكن تعديل هذه الرسالة؟ (نصّية، لي، غير محذوفة، وضمن المهلة) */
    fun canEdit(message: Message): Boolean {
        if (message.id.startsWith("tmp-")) return false
        if (message.sender?.id != _state.value.currentUserId) return false
        if (message.type != null && message.type != "text") return false
        if (message.isDeleted == true) return false
        val sentAt = parseIsoMillis(message.createdAt) ?: return true
        return System.currentTimeMillis() - sentAt <= editWindowMs
    }

    fun startEdit(message: Message) {
        if (!canEdit(message)) return
        _state.update { it.copy(editingMessage = message, reactionTarget = null) }
    }

    fun cancelEdit() {
        _state.update { it.copy(editingMessage = null) }
    }

    fun confirmEdit(newContent: String) {
        val target = _state.value.editingMessage ?: return
        val text = newContent.trim()
        if (text.isBlank() || text == target.content || _state.value.editSubmitting) return
        _state.update { it.copy(editSubmitting = true) }
        viewModelScope.launch {
            when (val r = messagesRepo.editMessage(target.id, text)) {
                is NetworkResult.Success -> {
                    // نعتمد نصّ الخادم لا نصّ المستخدم — قد يكون مكتوماً بفلتر الكلمات
                    _state.update { s ->
                        s.copy(
                            messages = s.messages.map {
                                if (it.id == target.id) it.copy(content = r.data, isEdited = true)
                                else it
                            },
                            editingMessage = null,
                            editSubmitting = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(editSubmitting = false) }
                    _message.tryEmit(ErrorMessages.friendly(r))
                }
            }
        }
    }

    private fun parseIsoMillis(iso: String?): Long? {
        iso ?: return null
        return runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(iso.take(19))?.time
        }.getOrNull()
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
                is NetworkResult.Success -> _message.tryEmit(S.get(R.string.msg_forwarded))
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

    // ── المحرّر الموحّد: صور وفيديو ─────────────────────────────

    fun previewImages(uris: List<android.net.Uri>, source: com.chathala.hala.feature.chats.ui.chat.components.ImageSource) {
        if (isMessagingLocked() || uris.isEmpty()) return
        _state.update { it.copy(pendingImages = uris, pendingImageSource = source) }
    }

    fun previewVideo(uri: android.net.Uri) {
        if (isMessagingLocked()) return
        _state.update { it.copy(pendingVideo = uri) }
    }

    fun dismissMediaEditor() {
        _state.update { it.copy(pendingImages = emptyList(), pendingVideo = null) }
    }

    /** صور المحرّر (JPEG ≤1MB مرسومة على الأصل) — كل صورة رسالة، والردّ على الأولى. */
    fun sendEditedPhotos(photos: List<com.chathala.hala.feature.editor.EditedPhoto>) {
        val source = if (_state.value.pendingImageSource ==
            com.chathala.hala.feature.chats.ui.chat.components.ImageSource.CAMERA) "camera" else "gallery"
        _state.update { it.copy(pendingImages = emptyList()) }
        if (!canSendNow()) return
        val reply = currentDraft().reply
        viewModelScope.launch {
            val dir = java.io.File(appContext.cacheDir, "images").apply { mkdirs() }
            photos.forEachIndexed { i, photo ->
                val file = java.io.File(dir, "edited_${System.nanoTime()}.jpg")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { file.writeBytes(photo.jpeg) }
                val base = outgoingMessage("image", mediaUrl = android.net.Uri.fromFile(file).toString())
                    .copy(imageSource = source, replyTo = if (i == 0) reply else null)
                enqueue(
                    if (photo.timerSec != null) base.copy(
                        disappearing = com.chathala.hala.feature.chats.data.DisappearingInfo(enabled = true, duration = photo.timerSec)
                    ) else base
                )
            }
            if (reply != null) cancelReply()
        }
    }

    /** فيديو مضغوط من المحرّر. فشل الرفع يبقى بفقاعة «لم يُرسل» بالملف المضغوط نفسه. */
    fun sendEditedVideo(video: com.chathala.hala.feature.editor.EditedVideo) {
        _state.update { it.copy(pendingVideo = null) }
        if (!canSendNow()) { video.output.file.delete(); return }
        val out = video.output
        val reply = currentDraft().reply
        enqueue(
            outgoingMessage("video", mediaUrl = android.net.Uri.fromFile(out.file).toString()).copy(
                videoDuration = out.durationSec,
                mediaWidth = out.width,
                mediaHeight = out.height,
                replyTo = reply,
                disappearing = com.chathala.hala.feature.chats.data.DisappearingInfo(
                    enabled = true,
                    mode = video.mode,
                    duration = kotlin.math.ceil(out.durationSec).toInt()
                )
            )
        )
        if (reply != null) cancelReply()
    }

    fun dismissVideoLimit() { _state.update { it.copy(videoLimitMessage = null) } }

    /**
     * فتح فيديو مؤقت: **نزّل الملف كاملاً قبل `view-photo`** (التسجيل يبدأ العدّ)، ثم سجّل
     * المشاهدة. المرسل يشاهد فيديوه دون تسجيل.
     */
    fun openVideo(message: Message) {
        val url = message.mediaUrl ?: return
        val id = message.id
        val s = _state.value
        if (id in s.expiredDisappearing || id in s.loadingDisappearing || s.playingVideo != null) return
        val isMine = message.sender?.id == s.currentUserId
        _state.update { it.copy(loadingDisappearing = it.loadingDisappearing + id) }
        viewModelScope.launch {
            val dir = java.io.File(appContext.cacheDir, "video_view").apply { mkdirs() }
            val file = java.io.File(dir, "$id.mp4")
            val downloaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val full = if (url.startsWith("http")) url else com.chathala.hala.core.network.ApiClient.BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
                    com.chathala.hala.core.network.ApiClient.httpClient
                        .newCall(okhttp3.Request.Builder().url(full).build()).execute().use { resp ->
                            check(resp.isSuccessful)
                            file.outputStream().use { out -> resp.body!!.byteStream().copyTo(out) }
                        }
                }.isSuccess
            }
            if (!downloaded) {
                file.delete()
                _state.update { it.copy(loadingDisappearing = it.loadingDisappearing - id) }
                _message.tryEmit(S.get(R.string.video_load_failed))
                return@launch
            }
            val once = message.disappearing?.mode != com.chathala.hala.feature.chats.data.VideoMode.DAY
            var deadline = messageTimestamp(message.disappearing?.expiresAt)
            if (!isMine) {
                when (val r = messagesRepo.viewDisappearingVideo(id)) {
                    is NetworkResult.Error -> {
                        file.delete()
                        _state.update { st ->
                            st.copy(
                                loadingDisappearing = st.loadingDisappearing - id,
                                expiredDisappearing = if (r.httpCode == 410) st.expiredDisappearing + id else st.expiredDisappearing
                            )
                        }
                        _message.tryEmit(if (r.httpCode == 410) S.get(R.string.video_state_expired) else ErrorMessages.friendly(r))
                        return@launch
                    }
                    is NetworkResult.Success -> deadline = messageTimestamp(r.data.expiresAt) ?: deadline
                }
            }
            _state.update {
                it.copy(
                    loadingDisappearing = it.loadingDisappearing - id,
                    viewedDisappearing = if (isMine) it.viewedDisappearing else it.viewedDisappearing + (id to (deadline ?: 0L)),
                    playingVideo = PlayingVideo(id, file, once, isMine, if (once) null else deadline)
                )
            }
        }
    }

    /** إغلاق العارض: «مرة واحدة» ← expire-photo ويُدمَّر محلياً. 24h يبقى قابلاً للإعادة. */
    fun closeVideo() {
        val playing = _state.value.playingVideo ?: return
        _state.update { it.copy(playingVideo = null) }
        if (playing.once && !playing.isMine) {
            markDisappearingExpired(playing.messageId)
            viewModelScope.launch { messagesRepo.expireDisappearingMedia(playing.messageId) }
        }
    }

    private fun markDisappearingExpired(messageId: String) {
        disappearingRemovalJobs.remove(messageId)?.cancel()
        _state.update { s ->
            s.copy(
                messages = s.messages.map { if (it.id == messageId) it.copy(mediaUrl = null) else it },
                viewedDisappearing = s.viewedDisappearing - messageId,
                expiredDisappearing = s.expiredDisappearing + messageId
            )
        }
    }

    /** بلاغ من داخل عارض الصورة المؤقتة/الفيديو — يُرفق المحتوى دليلاً. */
    fun reportMedia(messageId: String, reason: com.chathala.hala.feature.reporting.data.ReportReason) {
        val uid = _state.value.otherUserId ?: return
        viewModelScope.launch {
            when (val r = reportRepo.reportUser(uid, reason, messageId = messageId)) {
                is NetworkResult.Success -> _message.tryEmit(r.data)
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
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
            r.hoursLeft?.let { S.get(R.string.chat_locked_send_after, it) }
                ?: S.get(R.string.chat_locked_promo)
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

    private fun sendDisappearingImage(context: Context, uri: Uri, duration: Int, imageSource: String) {
        if (!canSendNow()) return
        enqueue(outgoingMessage("image", mediaUrl = uri.toString()).copy(
            imageSource = imageSource,
            disappearing = com.chathala.hala.feature.chats.data.DisappearingInfo(enabled = true, duration = duration)
        ))
    }

    /**
     * فتح صورة مؤقتة عند المستقبِل.
     *
     * المؤقّت **لا** يبدأ عند الضغط، بل بعد تحميل الصورة فعلياً إلى كاش Coil —
     * وإلا احتُسبت ثواني العرض على شاشة سوداء عند بطء الشبكة.
     */
    fun viewDisappearingPhoto(message: Message) {
        val duration = message.disappearing?.duration ?: return
        val url = message.mediaUrl ?: return
        if (message.id in _state.value.expiredDisappearing ||
            message.id in _state.value.viewedDisappearing ||
            message.id in _state.value.loadingDisappearing
        ) return

        _state.update { it.copy(loadingDisappearing = it.loadingDisappearing + message.id) }
        viewModelScope.launch {
            // 1) نزّل الصورة قبل فتح العرض
            try {
                val ready = preloadImage(url)
                if (!ready) {
                    _state.update { it.copy(loadingDisappearing = it.loadingDisappearing - message.id) }
                    _message.tryEmit(S.get(R.string.img_load_failed_retry))
                    return@launch
                }
                if (message.id in _state.value.expiredDisappearing) {
                    evictImageCache(url)
                    _state.update { it.copy(loadingDisappearing = it.loadingDisappearing - message.id) }
                    return@launch
                }
                // Start viewing only after both loading and server acceptance succeed.
                val result = messagesRepo.viewDisappearingPhoto(message.id)
                if (result is NetworkResult.Error || message.id in _state.value.expiredDisappearing) {
                    evictImageCache(url)
                    _state.update { it.copy(loadingDisappearing = it.loadingDisappearing - message.id) }
                    if (result is NetworkResult.Error) _message.tryEmit(ErrorMessages.friendly(result))
                    return@launch
                }
                // The current endpoint returns no deadline, so use the configured duration.
                val expiresAt = System.currentTimeMillis() + duration * 1000L
                _state.update {
                    it.copy(
                        loadingDisappearing = it.loadingDisappearing - message.id,
                        viewedDisappearing = it.viewedDisappearing + (message.id to expiresAt)
                    )
                }
                scheduleDisappearingRemoval(message.id, expiresAt)
            } catch (e: kotlinx.coroutines.CancellationException) {
                evictImageCache(url)
                throw e
            }
        }
    }

    /** ينزّل الصورة إلى كاش Coil ويُرجع true عند النجاح. */
    private suspend fun preloadImage(url: String): Boolean {
        val request = coil.request.ImageRequest.Builder(appContext)
            .data(url)
            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
            .memoryCacheKey(url)
            .build()
        val result = coil.Coil.imageLoader(appContext).execute(request)
        return result is coil.request.SuccessResult
    }

    /**
     * ينهي صلاحية الصورة المؤقتة عند [expiresAtMs]:
     * تبقى الفقاعة بحالة «انتهت»، ويُمسح الرابط وكاش الصورة فوراً.
     * يُمسح المحتوى عند الموعد المحدد دون تمديد مهلة العرض.
     * يُستخدم لكلا الطرفين (المستقبِل عند المشاهدة، والمرسِل عند وصول photo-viewed).
     */
    private fun scheduleDisappearingRemoval(messageId: String, expiresAtMs: Long) {
        disappearingRemovalJobs.remove(messageId)?.cancel()
        disappearingRemovalJobs[messageId] = viewModelScope.launch {
            delay((expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L))
            val url = _state.value.messages.firstOrNull { it.id == messageId }?.mediaUrl
            _state.update { s ->
                s.copy(
                    messages = s.messages.map {
                        if (it.id == messageId) it.copy(mediaUrl = null) else it
                    },
                    viewedDisappearing = s.viewedDisappearing - messageId,
                    expiredDisappearing = s.expiredDisappearing + messageId
                )
            }
            // امسح الصورة من الكاش فوراً — لا تبقى نسخة على الجهاز بعد الانتهاء
            url?.let { evictImageCache(it) }
            disappearingRemovalJobs.remove(messageId)
        }
    }

    private fun restoreDisappearingDeadlines() {
        _state.value.messages.filter { it.disappearing?.enabled == true }.forEach { message ->
            val deadline = messageTimestamp(message.disappearing?.expiresAt) ?: return@forEach
            // الفيديو: expiresAt حدّه الأقصى (24 ساعة) لا «فُتح» — نجدول الانتهاء فقط
            if (!message.isVideo()) {
                _state.update { it.copy(viewedDisappearing = it.viewedDisappearing + (message.id to deadline)) }
            }
            scheduleDisappearingRemoval(message.id, deadline)
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private fun evictImageCache(url: String) {
        runCatching {
            val loader = coil.Coil.imageLoader(appContext)
            loader.memoryCache?.remove(coil.memory.MemoryCache.Key(url))
            loader.diskCache?.remove(url)
        }
    }

    // ── External Promo Dialog ─────────────────────────────────────

    fun showExternalPromoInfo(message: Message) {
        val categories = message.externalPromoCategories ?: listOf("external_promo")
        _state.update {
            it.copy(
                externalPromoDialog = ExternalPromoBlockedInfo(
                    title = S.get(R.string.msg_blocked_title),
                    message = S.get(R.string.msg_blocked_desc),
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
                            _message.tryEmit(S.get(R.string.feature_unavailable))
                        code?.contains("USER_SETTING_DISABLED") == true ->
                            _message.tryEmit(S.get(R.string.enable_sensitive_first))
                        code?.contains("AGE_RESTRICTED") == true ->
                            _message.tryEmit(S.get(R.string.adults_only_18))
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
                _message.tryEmit(S.get(R.string.trust_enabled_msg))
            } else {
                appPreferences.untrustConversation(conversationId)
                _message.tryEmit(S.get(R.string.trust_disabled_msg))
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
                    _message.tryEmit(if (r.data) S.get(R.string.chat_muted) else S.get(R.string.chat_unmuted))
                    // حدّث بيانات المستخدم لتنعكس mute indicator فوراً في قائمة المحادثات
                    runCatching { userRepo.refresh() }
                }
                is NetworkResult.Error -> _message.tryEmit(ErrorMessages.friendly(r))
            }
        }
    }

    // ── Image ─────────────────────────────────────────────────────
    fun sendImage(context: Context, uri: Uri, imageSource: String = "gallery") {
        if (!canSendNow()) return
        enqueue(outgoingMessage("image", mediaUrl = uri.toString()).copy(imageSource = imageSource))
    }

    // ── Audio recording ───────────────────────────────────────────
    fun startRecording(context: Context) {
        if (_state.value.recording || !canSendNow()) return
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
                _message.tryEmit(S.serverOr(err.message, R.string.rec_start_failed))
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
                if (!canSendNow()) {
                    rec.file.delete()
                    return@fold
                }
                enqueue(outgoingMessage("audio", mediaUrl = rec.file.absolutePath).copy(
                    audioDuration = rec.durationSeconds.toDouble(), audioWaveform = rec.waveform
                ))
            },
            onFailure = { err ->
                _message.tryEmit(S.serverOr(err.message, R.string.rec_failed))
            }
        )
    }

    fun cancelRecording() {
        recordTimerJob?.cancel()
        recorder.cancel()
        _state.update { it.copy(recording = false, recordingSeconds = 0) }
    }

    override fun onCleared() {
        super.onCleared()
        com.chathala.hala.feature.push.InAppAlerts.setActiveConversation(null)
        _state.value.messages.filter { it.disappearing?.enabled == true }
            .mapNotNull { it.mediaUrl }.forEach(::evictImageCache)
        recordTimerJob?.cancel()
        recorder.cancel()
        audioPlayer.stop()
        stopTypingNow()
        socket.leaveConversation(conversationId)

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
                    appPreferences = AppPreferences(app),
                    appContext = app.applicationContext,
                    composerStorage = app.chatComposerStorage,
                    sendCoordinator = app.chatSendCoordinator
                )
            }
        }
    }
}
