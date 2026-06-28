package com.chathala.hala.feature.chats.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import coil.compose.AsyncImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.chathala.hala.core.ads.findActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chathala.hala.core.storage.AppPreferences
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.feature.chats.ui.chat.components.ChatInputBar
import com.chathala.hala.feature.chats.ui.chat.components.ChatMode
import com.chathala.hala.feature.chats.ui.chat.components.ChatModeDialog
import com.chathala.hala.feature.chats.ui.chat.components.ImagePreviewScreen
import com.chathala.hala.feature.chats.ui.chat.components.ForwardSheet
import com.chathala.hala.feature.chats.ui.chat.components.MessageBubble
import com.chathala.hala.feature.chats.ui.chat.components.MuteDialog
import com.chathala.hala.feature.chats.ui.chat.components.ReactionSheet
import com.chathala.hala.feature.chats.ui.chat.components.ReplyPreviewBar
import com.chathala.hala.feature.chats.ui.chat.components.ChatListItem
import com.chathala.hala.feature.chats.ui.chat.components.buildChatList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost
import com.chathala.hala.ui.theme.contrastBorderColor

@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOpenRequestPreview: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onOpenContentSettings: () -> Unit = {},
    onOpenRequests: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(conversationId))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.audioPlayer.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHost = rememberHalaSnackbarHost()
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val sensitiveContentEnabled by AppPreferences(context).sensitiveContentEnabled
        .collectAsStateWithLifecycle(initialValue = false)
    var menuExpanded by remember { mutableStateOf(false) }
    var showMuteDialog by remember { mutableStateOf(false) }
    var showChatModeDialog by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.conversationDeleted) {
        if (state.conversationDeleted) onBack()
    }

    // إعلان بيني عند فتح المحادثة — مرة كل نصف ساعة كحدّ أقصى
    LaunchedEffect(Unit) {
        com.chathala.hala.core.ads.InterstitialAdManager.preload(context)
        context.findActivity()?.let {
            com.chathala.hala.core.ads.InterstitialAdManager.maybeShowOnChatOpen(it)
        }
    }

    // إعادة توجيه: لو المحادثة pending وأنا المستلِم → افتح شاشة معاينة الطلب بدل الشات
    LaunchedEffect(state.conversationStatus, state.isCreator) {
        if (state.conversationStatus == "pending" && !state.isCreator) {
            onOpenRequestPreview(conversationId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    val coroutineScope = rememberCoroutineScope()
    val chatItems = remember(state.messages, state.firstUnreadId) {
        buildChatList(state.messages, state.firstUnreadId)
    }
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf true
            last >= info.totalItemsCount - 2
        }
    }
    val isAtTop by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 30)
        }
    }
    // Pagination: عند الوصول للأعلى نطلب الأقدم
    LaunchedEffect(isAtTop, state.hasMore, state.loadingOlder) {
        if (isAtTop && state.hasMore && !state.loadingOlder && state.messages.isNotEmpty()) {
            viewModel.loadOlder()
        }
    }
    // هل أُجري التمرير الأولي (لآخر رسالة/أول غير مقروءة) عند فتح المحادثة؟
    var initialScrolled by rememberSaveable(conversationId) { mutableStateOf(false) }

    // «كان المستخدم بالأسفل» قبل وصول رسالة جديدة.
    // لا يتأثر بإضافة الرسالة (التي تقلب isAtBottom مؤقتاً): نضبطه false فقط عند تمرير المستخدم فعلاً.
    var wasAtBottom by remember(conversationId) { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        snapshotFlow { isAtBottom to listState.isScrollInProgress }.collect { (atBottom, scrolling) ->
            if (atBottom) wasAtBottom = true
            else if (scrolling) wasAtBottom = false
        }
    }

    // Auto-scroll: تمرير أولي للقاع عند الفتح (إن لم يوجد هدف محدد)،
    // ثم تمرير عند رسالة جديدة: دائماً لو الرسالة منّي، أو لو كان المستخدم بالأسفل.
    LaunchedEffect(state.messages.size) {
        val msgs = state.messages
        if (msgs.isEmpty()) return@LaunchedEffect
        if (!initialScrolled) {
            // التمرير الأولي المستهدف (أول غير مقروءة/آخر رسالة) يتكفّل به الـ effect التالي.
            // إن لم يوجد هدف، نزل للقاع مباشرةً.
            if (state.scrollToMessageId == null) {
                androidx.compose.runtime.withFrameNanos { }
                listState.scrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                initialScrolled = true
            }
            return@LaunchedEffect
        }
        val isMine = msgs.lastOrNull()?.sender?.id == state.currentUserId
        if (isMine || wasAtBottom) {
            androidx.compose.runtime.withFrameNanos { } // انتظر قياس العنصر الجديد
            listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
        }
    }
    // التمرير المستهدف عند الفتح (أول غير مقروءة أو آخر رسالة) أو قفزة الردّ.
    // نضيف إزاحة العناصر القيادية (إشعار الخصوصية + مُحمّل الأقدم) لأن فهرس
    // chatItems يختلف عن فهرس الـ LazyColumn الفعلي.
    LaunchedEffect(state.scrollToMessageId, chatItems) {
        val targetId = state.scrollToMessageId ?: return@LaunchedEffect
        val idx = chatItems.indexOfFirst {
            it is ChatListItem.MessageItem && it.message.id == targetId
        }
        if (idx >= 0) {
            androidx.compose.runtime.withFrameNanos { } // انتظر استقرار التخطيط
            val total = listState.layoutInfo.totalItemsCount
            val isLast = idx == chatItems.lastIndex
            if (isLast) {
                // آخر رسالة → انزل للقاع الحقيقي (يشمل الـ Spacer الأخير)
                listState.scrollToItem((total - 1).coerceAtLeast(0))
            } else {
                // قفزة لرسالة وسطية → صحّح بإزاحة العناصر القيادية
                val leadingOffset = (total - chatItems.size - 1).coerceAtLeast(0)
                listState.scrollToItem((idx + leadingOffset).coerceAtLeast(0))
            }
            initialScrolled = true
            viewModel.consumeScrollTarget()
        }
    }
    // إعادة تصفير العدّاد عند الوصول للأسفل
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && state.newIncomingCount > 0) viewModel.resetNewIncoming()
    }

    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            // الضغط خارج الحقل يُخفي لوحة المفاتيح
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            ChatHeader(
                title = state.otherUserName ?: "محادثة",
                avatarUrl = state.otherUserAvatar,
                isOnline = state.otherUserOnline,
                isVerified = state.otherUserVerified,
                subtitle = when {
                    state.typingUser != null -> "يكتب"
                    state.otherUserOnline -> "متصل الآن"
                    state.socketConnected -> "متصل"
                    else -> "…"
                },
                subtitleTint = when {
                    state.typingUser != null -> MaterialTheme.colorScheme.primary
                    state.otherUserOnline -> MaterialTheme.colorScheme.primary
                    state.socketConnected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                isTyping = state.typingUser != null,
                chatModeIcon = ChatMode.fromApi(state.chatMode).icon,
                onBack = onBack,
                isTrusted = state.isTrustedConversation,
                onAvatarClick = {
                    state.otherUserId?.let { onOpenUserProfile(it) }
                },
                onChatModeClick = { showChatModeDialog = true },
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = !menuExpanded },
                onMenuDismiss = { menuExpanded = false },
                onOpenMute = {
                    menuExpanded = false
                    showMuteDialog = true
                },
                onReport = {
                    menuExpanded = false
                    showReportSheet = true
                },
                onBlock = {
                    menuExpanded = false
                    showBlockDialog = true
                },
                onDelete = {
                    menuExpanded = false
                    showDeleteDialog = true
                },
                onToggleTrust = {
                    menuExpanded = false
                    viewModel.toggleTrustConversation()
                }
            )

            // بانر تقييد المراسلة بسبب نشر حسابات خارجية
            state.messagingRestriction?.let { restriction ->
                MessagingRestrictionBanner(restriction)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    state.loading -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    state.error != null -> ErrorState(
                        message = state.error ?: "",
                        onRetry = viewModel::loadMessages
                    )
                    state.messages.isEmpty() -> EmptyChat()
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (state.loadingOlder) {
                            item("older-loader") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        item("privacy-notice") {
                            ChatPrivacyNotice()
                        }
                        items(chatItems, key = { it.key }) { item ->
                            when (item) {
                                is ChatListItem.DateHeader -> DateHeaderRow(
                                    item.label,
                                    Modifier.animateItem()
                                )
                                is ChatListItem.UnreadDivider -> UnreadDividerRow(Modifier.animateItem())
                                is ChatListItem.MessageItem -> {
                                    val msg = item.message
                                    val isMine = msg.sender?.id == state.currentUserId
                                    val isPending = msg.id.startsWith("tmp-")
                                    // تجميع: مسافة أكبر فوق أول رسالة في المجموعة، أضيق داخلها
                                    val topPad = if (item.isFirstInGroup) 6.dp else 0.dp
                                    Box(Modifier.animateItem().padding(top = topPad)) {
                                    com.chathala.hala.feature.chats.ui.chat.components.SwipeToReply(
                                        onReply = { viewModel.startReply(msg) },
                                        enabled = !isPending,
                                        content = {
                                            MessageBubble(
                                                message = msg,
                                                isMine = isMine,
                                                currentUserId = state.currentUserId,
                                                isPending = isPending,
                                                audioPlayingId = playback.playingId,
                                                audioPositionMs = playback.positionMs,
                                                audioDurationMs = playback.durationMs,
                                                disappearingExpiresAtMs = state.viewedDisappearing[msg.id],
                                                onToggleAudio = { m ->
                                                    m.mediaUrl?.let { url ->
                                                        viewModel.audioPlayer.toggle(m.id, url)
                                                    }
                                                },
                                                onLongPress = { m ->
                                                    haptic.performHapticFeedback(
                                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.openReactionSheet(m)
                                                },
                                                onViewDisappearing = { m -> viewModel.viewDisappearingPhoto(m) },
                                                onReplyTap = { replyId -> viewModel.jumpToMessage(replyId) },
                                                revealedContent = state.revealedSensitive[msg.id]?.let {
                                                    if (it.expiresAt > System.currentTimeMillis()) it.content else null
                                                },
                                                onRevealSensitive = { m ->
                                                    if (sensitiveContentEnabled) viewModel.revealSensitiveMessage(m)
                                                    else onOpenContentSettings()
                                                },
                                                onShowExternalPromoInfo = { m -> viewModel.showExternalPromoInfo(m) }
                                            )
                                        }
                                    )
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                }

                // زر "↓ رسائل جديدة" العائم
                if (!isAtBottom && state.newIncomingCount > 0) {
                    NewMessagesPill(
                        count = state.newIncomingCount,
                        onClick = {
                            coroutineScope.launch {
                                val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                listState.animateScrollToItem(lastIndex)
                            }
                            viewModel.resetNewIncoming()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }

                // زر «النزول للأسفل» الدائم — يظهر كلما تصفّح المستخدم للأعلى (بلا رسائل جديدة)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isAtBottom && state.newIncomingCount == 0,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                ) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                listState.animateScrollToItem(lastIndex)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "النزول للأسفل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                }
            }

            state.replyingTo?.let { target ->
                ReplyPreviewBar(
                    replyingTo = target,
                    onCancel = viewModel::cancelReply
                )
            }

            // تحذير استباقي عند الكتابة (يُخفى عند التقييد الكامل)
            AnimatedVisibility(
                visible = state.promoWarningCategory != null && state.messagingRestriction == null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0xFFFF8C00).copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "مشاركة ${state.promoWarningCategory} قد تُحجَب رسالتك وتُسجَّل مخالفة",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color(0xFFFF8C00)
                    )
                }
            }

            when {
                // مقيّد بسبب نشر حسابات خارجية → شريط مقفل (لا كتابة/صور/تسجيل)
                state.messagingRestriction != null -> {
                    LockedInputBar(
                        restriction = state.messagingRestriction!!,
                        submitting = state.reviewSubmitting,
                        reviewRequested = state.reviewRequested,
                        onRequestReview = onOpenRequests
                    )
                }
                state.canSend -> {
                    ChatInputBar(
                        value = state.input,
                        onValueChange = viewModel::onInputChange,
                        onSend = viewModel::send,
                        onPickImage = { uri, source -> viewModel.previewImage(uri, source) },
                        onStartRecord = { viewModel.startRecording(context) },
                        onStopRecord = viewModel::stopAndSendRecording,
                        onCancelRecord = viewModel::cancelRecording,
                        isRecording = state.recording,
                        recordingSeconds = state.recordingSeconds,
                        enabled = !state.sending && !state.uploadingMedia
                    )
                }
                else -> {
                    PendingBanner(
                        status = state.conversationStatus,
                        isCreator = state.isCreator
                    )
                }
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    state.reactionTarget?.let { target ->
        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
        val copyText = target.content?.takeIf { it.isNotBlank() }
        ReactionSheet(
            messagePreview = when (target.type) {
                "image" -> "📷 صورة"
                "audio" -> "🎙️ رسالة صوتية"
                else -> target.content?.take(80) ?: ""
            },
            canDelete = target.sender?.id == state.currentUserId,
            canCopy = copyText != null,
            onPick = { emoji -> viewModel.react(target, emoji) },
            onReply = {
                viewModel.dismissReactionSheet()
                viewModel.startReply(target)
            },
            onCopy = {
                copyText?.let {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(it))
                }
                viewModel.dismissReactionSheet()
            },
            onForward = { viewModel.openForward(target) },
            onDelete = { viewModel.deleteMessage(target) },
            onDismiss = viewModel::dismissReactionSheet
        )
    }

    if (showMuteDialog) {
        MuteDialog(
            onDismiss = { showMuteDialog = false },
            onConfirm = { iso ->
                showMuteDialog = false
                viewModel.toggleMute(muted = true, mutedUntilIso = iso)
            }
        )
    }

    if (showChatModeDialog) {
        ChatModeDialog(
            current = state.chatMode,
            onPick = { mode ->
                showChatModeDialog = false
                viewModel.setChatMode(mode)
            },
            onDismiss = { showChatModeDialog = false }
        )
    }

    state.pendingImageUri?.let { uri ->
        ImagePreviewScreen(
            previewUri = uri,
            source = state.pendingImageSource,
            onSend = { duration ->
                viewModel.sendPendingImage(context, duration)
            },
            onDismiss = viewModel::dismissImagePreview
        )
    }

    state.forwardTarget?.let { _ ->
        val app = context.applicationContext as com.chathala.hala.HalaApp
        ForwardSheet(
            repo = app.conversationsRepository,
            currentUserId = state.currentUserId,
            excludeConversationId = conversationId,
            onPick = { convId -> viewModel.confirmForward(convId) },
            onDismiss = viewModel::dismissForward
        )
    }

    if (showReportSheet) {
        com.chathala.hala.feature.reporting.ui.ReportUserSheet(
            targetUserName = state.otherUserName,
            submitting = state.reporting,
            onSubmit = { reason, desc ->
                showReportSheet = false
                viewModel.reportOtherUser(reason, desc)
            },
            onDismiss = { showReportSheet = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.deleting) showDeleteDialog = false },
            title = { Text("حذف المحادثة") },
            text = { Text("سيتم حذف المحادثة من قائمتك فقط. هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteConversation()
                    },
                    enabled = !state.deleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.blocking) showBlockDialog = false },
            title = { Text("حظر المستخدم") },
            text = {
                Text("هل أنت متأكد من حظر ${state.otherUserName ?: "هذا المستخدم"}؟ لن يتمكن من مراسلتك أو رؤية ملفك.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockDialog = false
                        viewModel.blockOtherUser()
                    },
                    enabled = !state.blocking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("حظر") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("إلغاء") }
            }
        )
    }

    state.externalPromoDialog?.let { info ->
        com.chathala.hala.feature.chats.ui.chat.components.ExternalPromoBlockedDialog(
            info = info,
            onDismiss = viewModel::dismissExternalPromoDialog,
            onAppeal = { reason -> viewModel.appealMessageBlock(
                state.messages.lastOrNull { it.isExternalPromoBlocked == true } ?: return@ExternalPromoBlockedDialog,
                reason
            )}
        )
    }
}

@Composable
private fun ChatHeader(
    title: String,
    avatarUrl: String?,
    isOnline: Boolean,
    isVerified: Boolean,
    subtitle: String,
    subtitleTint: androidx.compose.ui.graphics.Color,
    isTyping: Boolean = false,
    chatModeIcon: String,
    onBack: () -> Unit,
    isTrusted: Boolean = false,
    onAvatarClick: () -> Unit,
    onChatModeClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onMenuDismiss: () -> Unit,
    onOpenMute: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    onToggleTrust: () -> Unit = {}
) {
    val borderColor = contrastBorderColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // حدّ سفلي فقط بدل الإطار الكامل
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = borderColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height - stroke / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height - stroke / 2),
                    strokeWidth = stroke
                )
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Avatar + dot الاتصال
        Box(modifier = Modifier.size(40.dp).clickable(onClick = onAvatarClick)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = title.take(1).ifBlank { "?" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(0xFF4CAF50))
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clickable(onClick = onAvatarClick)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isVerified) {
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleTint
                )
                if (isTyping) {
                    Spacer(Modifier.size(4.dp))
                    com.chathala.hala.ui.components.TypingDots(color = subtitleTint, dotSize = 5)
                }
            }
        }
        IconButton(onClick = onChatModeClick) {
            Text(
                text = chatModeIcon,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Box {
            IconButton(onClick = onMenuToggle) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onMenuDismiss
            ) {
                DropdownMenuItem(
                    text = { Text("كتم الإشعارات") },
                    onClick = onOpenMute,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("الإبلاغ عن المستخدم") },
                    onClick = onReport,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("حظر المستخدم", color = MaterialTheme.colorScheme.error)
                    },
                    onClick = onBlock,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "إغلاق المحادثة",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = onDelete,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isTrusted) "إلغاء الثقة بالمحادثة" else "ثقة بهذه المحادثة"
                        )
                    },
                    onClick = onToggleTrust,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = if (isTrusted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DateHeaderRow(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UnreadDividerRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Text(
            text = "الرسائل غير المقروءة",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun NewMessagesPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "$count رسائل جديدة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun PendingBanner(status: String?, isCreator: Boolean) {
    val text = when {
        status == "rejected" -> "تم رفض هذه المحادثة"
        status == "expired" -> "انتهت صلاحية الطلب"
        status == "pending" && isCreator -> "في انتظار قبول الطرف الآخر…"
        status == "pending" -> "اقبل الطلب أولاً للردّ"
        else -> "لا يمكن الردّ حالياً"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessagingRestrictionBanner(
    restriction: com.chathala.hala.feature.chats.data.MessagingRestrictionInfo
) {
    val orange = androidx.compose.ui.graphics.Color(0xFFFF8C00)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(orange.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🚫", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = restriction.title ?: "حسابك مقيّد مؤقتاً",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = orange
            )
            Text(
                text = restriction.message
                    ?: "تم تقييد المراسلة بسبب تكرار نشر حسابات خارجية.",
                style = MaterialTheme.typography.labelSmall,
                color = orange.copy(alpha = 0.85f)
            )
        }
        restriction.hoursLeft?.let { hrs ->
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(orange.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$hrs ساعة",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = orange
                )
            }
        }
    }
}

@Composable
private fun LockedInputBar(
    restriction: com.chathala.hala.feature.chats.data.MessagingRestrictionInfo,
    submitting: Boolean,
    reviewRequested: Boolean,
    onRequestReview: () -> Unit
) {
    val orange = androidx.compose.ui.graphics.Color(0xFFFF8C00)
    val reviewPending = reviewRequested
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = orange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "المراسلة مقفلة مؤقتاً",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = orange
                )
                Text(
                    text = restriction.hoursLeft?.let { "يمكنك الإرسال بعد $it ساعة" }
                        ?: "تم تقييد المراسلة بسبب نشر حسابات خارجية",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.size(10.dp))

        if (reviewPending) {
            // طلب مراجعة سبق إرساله
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(orange.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HourglassEmpty,
                    contentDescription = null,
                    tint = orange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "طلب المراجعة قيد المعالجة لدى المشرف",
                    style = MaterialTheme.typography.labelMedium,
                    color = orange
                )
            }
        } else {
            OutlinedButton(
                onClick = onRequestReview,
                enabled = !submitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = orange,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.RateReview,
                        contentDescription = null,
                        tint = orange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "طلب مراجعة",
                        style = MaterialTheme.typography.labelLarge,
                        color = orange
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatPrivacyNotice() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔒",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(androidx.compose.ui.Modifier.size(4.dp))
            Text(
                text = "محادثتك سرية",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "لا يمكن الاطلاع عليها ما لم تقم بمشاركتها",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyChat() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ابدأ الحديث بقول مرحباً 👋",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

