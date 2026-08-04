package com.chathala.hala.feature.friends.ui

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.chathala.hala.feature.friends.data.FriendItem
import com.chathala.hala.feature.friends.data.FriendRequestItem
import com.chathala.hala.feature.friends.data.FriendUser
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    viewModel: FriendsViewModel = viewModel(factory = FriendsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var removeTarget by remember { mutableStateOf<FriendItem?>(null) }

    LaunchedEffect(Unit) { viewModel.message.collect { snackbarHost.showSnackbar(it) } }
    LaunchedEffect(Unit) { viewModel.openConversation.collect { onOpenConversation(it) } }

    removeTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removeTarget = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(S.get(R.string.friends_remove_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(S.get(R.string.friends_remove_confirm, target.user.name ?: S.get(R.string.label_this_user)))
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.removeFriend(target)
                    removeTarget = null
                }) {
                    Text(S.get(R.string.action_remove), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { removeTarget = null }) { Text(S.get(R.string.action_undo)) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(onBack = onBack)
            TabRow(
                selected = state.tab,
                friendsCount = state.friends.size,
                requestsCount = state.requests.size,
                onSelect = viewModel::selectTab
            )

            val isFriendsTab = state.tab == FriendsTab.FRIENDS
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.error != null && state.friends.isEmpty() && state.requests.isEmpty() ->
                    ErrorState(message = state.error ?: "", onRetry = viewModel::load)

                isFriendsTab && state.friends.isEmpty() -> EmptyState(
                    icon = Icons.Filled.People,
                    title = S.get(R.string.friends_empty_title),
                    subtitle = S.get(R.string.friends_empty_desc)
                )
                !isFriendsTab && state.requests.isEmpty() -> EmptyState(
                    icon = Icons.Filled.PersonAddAlt,
                    title = S.get(R.string.friend_requests_empty_title),
                    subtitle = S.get(R.string.friend_requests_empty_desc)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isFriendsTab) {
                        items(state.friends, key = { it.friendshipId }) { friend ->
                            FriendRow(
                                friend = friend,
                                processing = friend.user.id in state.processingIds,
                                onOpenProfile = { onOpenUserProfile(friend.user.id) },
                                onChat = { viewModel.openChat(friend) },
                                onRemove = { removeTarget = friend }
                            )
                        }
                    } else {
                        items(state.requests, key = { it.friendshipId }) { req ->
                            RequestRow(
                                request = req,
                                processing = req.friendshipId in state.processingIds,
                                onOpenProfile = { onOpenUserProfile(req.user.id) },
                                onAccept = { viewModel.accept(req) },
                                onDecline = { viewModel.decline(req) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = S.get(R.string.friends_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun TabRow(
    selected: FriendsTab,
    friendsCount: Int,
    requestsCount: Int,
    onSelect: (FriendsTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabPill(S.get(R.string.friends_title), friendsCount, selected == FriendsTab.FRIENDS) { onSelect(FriendsTab.FRIENDS) }
        TabPill(S.get(R.string.friend_requests_title), requestsCount, selected == FriendsTab.REQUESTS) { onSelect(FriendsTab.REQUESTS) }
    }
}

@Composable
private fun TabPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = fg)
        if (count > 0) {
            Box(
                modifier = Modifier.clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("$count", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun FriendRow(
    friend: FriendItem,
    processing: Boolean,
    onOpenProfile: () -> Unit,
    onChat: () -> Unit,
    onRemove: () -> Unit
) {
    RowCard(onClick = onOpenProfile) {
        Avatar(friend.user)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            NameRow(friend.user)
            Spacer(Modifier.size(3.dp))
            Text(
                text = if (friend.user.isOnline == true) S.get(R.string.status_online_now) else S.get(R.string.status_offline),
                style = MaterialTheme.typography.labelMedium,
                color = if (friend.user.isOnline == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // مراسلة
        if (friend.conversationId != null) {
            CircleBtn(Icons.AutoMirrored.Filled.Chat, MaterialTheme.colorScheme.primary, enabled = !processing, onClick = onChat)
            Spacer(Modifier.width(8.dp))
        }
        // إزالة
        CircleBtn(Icons.Filled.Close, MaterialTheme.colorScheme.error, enabled = !processing, onClick = onRemove)
    }
}

@Composable
private fun RequestRow(
    request: FriendRequestItem,
    processing: Boolean,
    onOpenProfile: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    RowCard(onClick = onOpenProfile) {
        Avatar(request.user)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            NameRow(request.user)
            Spacer(Modifier.size(3.dp))
            Text(S.get(R.string.friend_wants_to_add), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (processing) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
        } else {
            CircleBtn(Icons.Filled.Check, MaterialTheme.colorScheme.primary, onClick = onAccept)
            Spacer(Modifier.width(8.dp))
            CircleBtn(Icons.Filled.Close, MaterialTheme.colorScheme.error, onClick = onDecline)
        }
    }
}

@Composable
private fun RowCard(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun NameRow(user: FriendUser) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = user.name ?: S.get(R.string.label_user),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        if (user.isVerified == true) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
        if (user.isPremium == true) {
            Spacer(Modifier.width(4.dp))
            Text("👑", fontSize = 13.sp)
        }
    }
}

@Composable
private fun Avatar(user: FriendUser) {
    Box(
        modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!user.profileImage.isNullOrBlank()) {
            AsyncImage(model = user.profileImage, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        } else {
            Text(user.name?.take(1) ?: "?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint.copy(alpha = if (enabled) 1f else 0.5f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
