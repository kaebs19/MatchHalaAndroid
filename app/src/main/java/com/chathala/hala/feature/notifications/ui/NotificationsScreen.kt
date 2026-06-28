package com.chathala.hala.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.feature.notifications.ui.components.NotificationRow
import com.chathala.hala.feature.notifications.ui.components.NotificationsEmpty
import com.chathala.hala.feature.notifications.ui.components.NotificationsSkeleton
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import com.chathala.hala.feature.notifications.data.NotificationItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onOpenConversation: (String) -> Unit = {},
    onOpenRequestPreview: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationsTopBar(
                unreadCount = state.unreadCount,
                canMarkAllRead = state.unreadCount > 0 && !state.bulkWorking,
                canDeleteAll = state.items.isNotEmpty() && !state.bulkWorking,
                onMarkAllRead = viewModel::markAllRead,
                onDeleteAll = { showDeleteAllDialog = true }
            )

            FilterBar(
                selected = state.filter,
                onSelect = viewModel::selectFilter
            )

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    state.initialLoading -> NotificationsSkeleton()

                    state.error != null -> ErrorState(
                        message = state.error ?: "",
                        onRetry = viewModel::retry
                    )

                    state.items.isEmpty() -> NotificationsEmpty(filter = state.filter)

                    else -> NotificationsList(
                        state = state,
                        onItemClick = { item ->
                            // علّم كمقروء، ثم انتقل حسب نوع الإشعار
                            viewModel.markRead(item.id)
                            val convId = (item.data?.get("conversationId") as? String)
                                ?: (item.data?.get("conversation") as? String)
                            val senderId = (item.data?.get("senderId") as? String)
                                ?: (item.data?.get("userId") as? String)
                                ?: item.sender?.id
                            when (item.type) {
                                "conversation_request", "super_like" -> {
                                    if (!convId.isNullOrBlank()) onOpenRequestPreview(convId)
                                }
                                "conversation_accepted",
                                "new_message",
                                "chat_mode_changed",
                                "conversation_reminder",
                                "flagged_message" -> {
                                    if (!convId.isNullOrBlank()) onOpenConversation(convId)
                                }
                                "new_match",
                                "new_like", "like",
                                "profile_view",
                                "new_follower" -> {
                                    if (!senderId.isNullOrBlank()) onOpenUserProfile(senderId)
                                }
                                else -> Unit
                            }
                        },
                        onDelete = viewModel::delete,
                        onLoadMore = viewModel::loadMore
                    )
                }
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.notifications_delete_all_title)) },
            text = { Text(stringResource(R.string.notifications_delete_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAll()
                    }
                ) { Text(stringResource(R.string.notifications_delete_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun NotificationsTopBar(
    unreadCount: Int,
    canMarkAllRead: Boolean,
    canDeleteAll: Boolean,
    onMarkAllRead: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.notifications_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (unreadCount > 0) {
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                com.chathala.hala.ui.components.AnimatedCounter(
                    count = unreadCount,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onMarkAllRead,
            enabled = canMarkAllRead
        ) {
            Icon(
                imageVector = Icons.Filled.DoneAll,
                contentDescription = stringResource(R.string.notifications_mark_all_read),
                tint = if (canMarkAllRead)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
        IconButton(
            onClick = onDeleteAll,
            enabled = canDeleteAll
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = stringResource(R.string.notifications_delete_all),
                tint = if (canDeleteAll)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun FilterBar(
    selected: NotificationFilter,
    onSelect: (NotificationFilter) -> Unit
) {
    val filters = NotificationFilter.entries
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { f ->
            val labelRes = when (f) {
                NotificationFilter.ALL -> R.string.notifications_filter_all
                NotificationFilter.UNREAD -> R.string.notifications_filter_unread
                NotificationFilter.SOCIAL -> R.string.notifications_filter_social
                NotificationFilter.SYSTEM -> R.string.notifications_filter_system
            }
            FilterChip(
                selected = f == selected,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(f)
                },
                label = { Text(stringResource(labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun NotificationsList(
    state: NotificationsUiState,
    onItemClick: (com.chathala.hala.feature.notifications.data.NotificationItem) -> Unit,
    onDelete: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= layout.totalItemsCount - 3
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { trigger ->
                if (trigger) onLoadMore()
            }
    }

    // track first-appearance per-item لعرض stagger animation مرة واحدة
    val appearedIds = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(items = state.items, key = { _, item -> item.id }) { index, item ->
            StaggerItem(
                key = item.id,
                index = index,
                appearedIds = appearedIds
            ) {
                SwipeToDeleteRow(
                    key = item.id,
                    onDelete = { onDelete(item.id) }
                ) {
                    NotificationRow(
                        item = item,
                        currentUserId = state.currentUserId,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
        if (state.loadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** يعرض انيميشن fade-in + slide up لأول ظهور فقط لكل إشعار. */
@Composable
private fun StaggerItem(
    key: String,
    index: Int,
    appearedIds: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    content: @Composable () -> Unit
) {
    val alreadyAppeared = appearedIds[key] == true
    var visible by remember(key) { androidx.compose.runtime.mutableStateOf(alreadyAppeared) }
    LaunchedEffect(key) {
        if (!alreadyAppeared) {
            delay((index.coerceAtMost(10) * 40L))
            visible = true
            appearedIds[key] = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )
    ) {
        content()
    }
}

/** يغلّف الصف بـ swipe-to-dismiss ثنائي الاتجاه مع حركة حذف ناعمة. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    key: String,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var confirmed by remember(key) { androidx.compose.runtime.mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.StartToEnd ||
                target == SwipeToDismissBoxValue.EndToStart
            ) {
                if (!confirmed) {
                    confirmed = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                true
            } else false
        },
        positionalThreshold = { it * 0.35f }
    )

    LaunchedEffect(confirmed) {
        if (confirmed) {
            delay(180)   // نترك الحركة تكتمل قليلاً
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(vertical = 2.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.notifications_delete_one),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    ) {
        content()
    }
}
