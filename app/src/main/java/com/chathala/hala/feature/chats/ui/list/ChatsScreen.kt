package com.chathala.hala.feature.chats.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import com.chathala.hala.core.ads.AdConfig
import com.chathala.hala.core.ads.BannerAd
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.feature.chats.ui.list.components.ChatsFilterChips
import com.chathala.hala.feature.chats.ui.list.components.ConversationActionsSheet
import com.chathala.hala.feature.chats.ui.list.components.ConversationRow
import com.chathala.hala.feature.chats.ui.list.components.SwipeableConversation
import com.chathala.hala.feature.reporting.ui.ReportUserSheet
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onOpenConversation: (String) -> Unit,
    onOpenRequestPreview: (String) -> Unit = {},
    onOpenRequests: () -> Unit = {},
    viewModel: ChatsViewModel = viewModel(factory = ChatsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var showReport by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    // عند العودة للشاشة: إعادة الانضمام لغرف السوكت + تحديث صامت
    // (شاشة المحادثة تُغادر الغرفة عند إغلاقها)
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            ChatsTopBar(
                searchOpen = state.searchOpen,
                searchQuery = state.searchQuery,
                onToggleSearch = viewModel::toggleSearch,
                onSearchChange = viewModel::setSearchQuery,
                socketConnected = state.socketConnected
            )

            if (!state.searchOpen) {
                ChatsFilterChips(
                    selected = state.filter,
                    unreadCount = state.unreadCount,
                    pendingCount = state.pendingBadge,
                    pinnedCount = state.accepted.count { it.id in state.pinnedIds },
                    premiumCount = state.accepted.count { conv ->
                        conv.participants.any { it.id != state.currentUserId && it.isPremium == true }
                    },
                    onSelect = viewModel::setFilter,
                    onOpenRequests = onOpenRequests
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                when {
                    state.loading && state.visible.isEmpty() -> LoadingPlaceholder()
                    state.error != null && state.visible.isEmpty() -> ErrorState(
                        message = state.error ?: "",
                        onRetry = viewModel::load
                    )
                    state.visible.isEmpty() -> EmptyChats(state.filter, state.searchQuery.isNotBlank())
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(state.visible, key = { _, c -> c.id }) { index, conv ->
                            val isPinned = conv.id in state.pinnedIds
                            SwipeableConversation(
                                key = conv.id,
                                isPinned = isPinned,
                                onPin = { viewModel.togglePin(conv.id) },
                                onDelete = { pendingDelete = conv.id }
                            ) {
                                ConversationRow(
                                    conversation = conv,
                                    currentUserId = state.currentUserId,
                                    isMuted = conv.id in state.mutedConversationIds,
                                    isPinned = isPinned,
                                    onClick = {
                                        val isReceiverPending = conv.status == "pending" &&
                                            conv.creator != state.currentUserId
                                        if (isReceiverPending) onOpenRequestPreview(conv.id)
                                        else onOpenConversation(conv.id)
                                    },
                                    onLongPress = { viewModel.openActions(conv) }
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            // بانر بعد كل N محادثة
                            if ((index + 1) % AdConfig.CHAT_LIST_BANNER_EVERY == 0) {
                                BannerAd(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f),
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ── Long-press actions ──
    state.actionTarget?.let { target ->
        val other = target.participants.firstOrNull { it.id != state.currentUserId }
        ConversationActionsSheet(
            targetName = other?.name,
            isPinned = target.id in state.pinnedIds,
            isMuted = target.id in state.mutedConversationIds,
            onPin = { viewModel.togglePin(target.id) },
            onMute = { viewModel.toggleMute(target.id) },
            onDelete = {
                viewModel.dismissActions()
                pendingDelete = target.id
            },
            onReport = {
                viewModel.dismissActions()
                showReport = true
            },
            onBlock = { viewModel.blockOther(target) },
            onDismiss = viewModel::dismissActions
        )
    }

    // ── Delete confirmation ──
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف المحادثة") },
            text = { Text("سيتم حذف المحادثة من قائمتك فقط. هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // ── Report sheet ──
    if (showReport) {
        val target = state.actionTarget ?: state.visible.firstOrNull()
        val other = target?.participants?.firstOrNull { it.id != state.currentUserId }
        ReportUserSheet(
            targetUserName = other?.name,
            submitting = false,
            onSubmit = { reason, desc ->
                showReport = false
                target?.let { viewModel.reportOther(it, reason, desc) }
            },
            onDismiss = { showReport = false }
        )
    }
}

@Composable
private fun ChatsTopBar(
    searchOpen: Boolean,
    searchQuery: String,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    socketConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searchOpen) {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "إغلاق البحث",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("ابحث في المحادثات…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            )
        } else {
            Text(
                text = "المحادثات",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (!socketConnected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC107))
                )
                Spacer(Modifier.size(8.dp))
            }
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyChats(filter: ChatsFilter, isSearching: Boolean) {
    val (title, body) = when {
        isSearching -> "لا توجد نتائج" to "حاول البحث بكلمة أخرى."
        filter == ChatsFilter.UNREAD -> "لا توجد محادثات غير مقروءة" to "قرأتَ كل شيء — أحسنت!"
        filter == ChatsFilter.PINNED -> "لا توجد محادثات مثبّتة" to "اسحب يميناً على أي محادثة لتثبيتها."
        filter == ChatsFilter.PREMIUM -> "لا توجد محادثات مع مستخدمين مميزين" to "ستظهر هنا المحادثات مع حاملي 👑."
        else -> "لا توجد محادثات بعد" to "ابدأ من شاشة الاكتشاف وابعث طلب محادثة جديد."
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Filled.Search
                          else Icons.AutoMirrored.Rounded.Chat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
