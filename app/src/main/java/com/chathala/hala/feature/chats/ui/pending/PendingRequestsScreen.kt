package com.chathala.hala.feature.chats.ui.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chathala.hala.feature.chats.data.PendingRequest
import com.chathala.hala.feature.chats.ui.pending.components.GreetingDialog
import com.chathala.hala.feature.notifications.util.NotificationFormat
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsScreen(
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenRequestPreview: (String) -> Unit = {},
    viewModel: PendingRequestsViewModel = viewModel(factory = PendingRequestsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var greetingTarget by remember { mutableStateOf<PendingRequest?>(null) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.acceptedEvent.collect { evt -> onOpenConversation(evt.conversationId) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(onBack = onBack)
            TabRow(
                selected = state.tab,
                receivedCount = state.receivedCount,
                sentCount = state.sentCount,
                onSelect = viewModel::selectTab
            )

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                when {
                    state.loading && state.items.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                    state.error != null && state.items.isEmpty() -> ErrorState(
                        message = state.error ?: "",
                        onRetry = viewModel::load
                    )

                    state.items.isEmpty() -> EmptyPending(state.tab)

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(state.items, key = { it.id }) { req ->
                            RequestListItem(
                                request = req,
                                isSent = state.tab == PendingTab.SENT,
                                isProcessing = req.id in state.processingIds,
                                onOpen = { onOpenRequestPreview(req.id) },
                                onCancelSent = { viewModel.cancelSent(req.id) }
                            )
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

    greetingTarget?.let { target ->
        GreetingDialog(
            senderName = target.creator?.name,
            onDismiss = { greetingTarget = null },
            onConfirm = { greeting ->
                viewModel.accept(target.id, greeting = greeting)
                greetingTarget = null
            }
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "طلبات المحادثة",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun TabRow(
    selected: PendingTab,
    receivedCount: Int,
    sentCount: Int,
    onSelect: (PendingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabPill(
            label = "مستلمة",
            count = receivedCount,
            selected = selected == PendingTab.RECEIVED,
            onClick = { onSelect(PendingTab.RECEIVED) }
        )
        TabPill(
            label = "مرسلة",
            count = sentCount,
            selected = selected == PendingTab.SENT,
            onClick = { onSelect(PendingTab.SENT) }
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = fg
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.primary
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun RequestListItem(
    request: PendingRequest,
    isSent: Boolean,
    isProcessing: Boolean,
    onOpen: () -> Unit,
    onCancelSent: () -> Unit
) {
    val creator = request.creator
    val isPremium = creator?.isPremium == true
    val ringColor = if (isPremium) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingAvatar(url = creator?.profileImage, ringColor = ringColor, name = creator?.name)

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NewBadge()
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = creator?.name ?: "مستخدم",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    if (creator?.isVerified == true) {
                        Spacer(Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text = if (isSent) "بانتظار الرد" else "يريد التحدث معك",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                // معاينة الرسالة الأولية إن وُجدت
                val initial = request.initialMessage?.content?.takeIf { it.isNotBlank() }
                if (!initial.isNullOrBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.size(2.dp))
                Text(
                    text = NotificationFormat.timeAgoArabic(request.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // المرسَلة فقط: زر إلغاء صغير. المستلَمة: نقرة على الصف تفتح المعاينة.
            if (isSent) {
                Spacer(Modifier.size(8.dp))
                CircleActionButton(
                    icon = Icons.Filled.Close,
                    bg = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    fg = MaterialTheme.colorScheme.error,
                    enabled = !isProcessing,
                    onClick = onCancelSent
                )
            }
        }
    }
}

@Composable
private fun NewBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "جديد",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    fg: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RingAvatar(url: String?, ringColor: Color, name: String?) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = ringColor, shape = CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name?.take(1) ?: "?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPending(tab: PendingTab) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (tab == PendingTab.RECEIVED) "لا توجد طلبات مستلمة" else "لا توجد طلبات مرسلة",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (tab == PendingTab.RECEIVED)
                "ستظهر هنا طلبات المحادثة الواردة إليك."
            else
                "الطلبات التي ترسلها للمستخدمين تظهر هنا حتى يتم الرد عليها.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
