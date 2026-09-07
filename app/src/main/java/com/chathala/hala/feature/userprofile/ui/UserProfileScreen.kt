package com.chathala.hala.feature.userprofile.ui

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import com.chathala.hala.feature.friends.data.FriendStatus
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.util.HapticHelper
import com.chathala.hala.core.util.ProfileFormatter
import com.chathala.hala.core.util.Zodiac
import com.chathala.hala.feature.discover.ui.components.QuickMessagesRow
import com.chathala.hala.feature.reporting.ui.ReportUserSheet
import com.chathala.hala.feature.userprofile.data.UserProfile
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost
import com.chathala.hala.ui.components.HalaAsyncImage

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    viewModel: UserProfileViewModel = viewModel(factory = UserProfileViewModel.factory(userId))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var showMessageSheet by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showFriendRemoveConfirm by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.openConversation.collect { convId ->
            showMessageSheet = false
            onOpenConversation(convId)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.loading -> ProfileSkeleton(onBack = onBack)

            // الحساب محذوف → لا زر إعادة محاولة (لن يعود أبداً)
            state.accountDeleted -> DeletedAccountState(onBack = onBack)

            state.error != null && state.user == null -> ErrorState(
                message = state.error ?: "",
                onRetry = viewModel::load
            )

            state.user != null -> ProfileContent(
                user = state.user!!,
                currentUserPremium = state.currentUserPremium,
                onBack = onBack,
                onMessage = { showMessageSheet = true },
                onLike = viewModel::likeUser,
                onSuperLike = viewModel::superLikeUser,
                onSkip = onBack,
                onReport = { showReport = true },
                onBlock = { showBlockConfirm = true },
                onUnblock = { viewModel.unblockUser() },
                onOpenPhoto = { viewerIndex = it },
                liked = state.liked,
                blocked = state.blocked,
                reported = state.reported,
                friendStatus = state.friendStatus,
                friendWorking = state.friendWorking,
                onAddFriend = viewModel::addFriend,
                onAcceptFriend = viewModel::acceptFriend,
                // إلغاء طلب / إزالة صديق / رفض طلب → تأكيد أولاً
                onRemoveFriend = { showFriendRemoveConfirm = true }
            )
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)
        )
    }

    viewerIndex?.let { idx ->
        FullscreenPhotoViewer(
            urls = state.user?.galleryUrls ?: emptyList(),
            initialIndex = idx,
            onDismiss = { viewerIndex = null }
        )
    }

    if (showMessageSheet && state.user != null) {
        MessageBottomSheet(
            targetName = state.user?.name,
            sending = state.requesting,
            alreadySent = state.requestSent,
            onSend = { msg ->
                viewModel.sendRequest(msg, isSuperLike = false)
                showMessageSheet = false
            },
            onDismiss = { showMessageSheet = false }
        )
    }

    if (showReport && state.user != null) {
        ReportUserSheet(
            targetUserName = state.user?.name,
            submitting = state.reporting,
            onSubmit = { reason, desc ->
                viewModel.reportUser(reason, desc)
                showReport = false
            },
            onDismiss = { showReport = false }
        )
    }

    if (showBlockConfirm && state.user != null) {
        BlockConfirmDialog(
            userName = state.user?.name,
            blocking = state.blocking,
            onConfirm = {
                viewModel.blockUser()
                showBlockConfirm = false
            },
            onDismiss = { showBlockConfirm = false }
        )
    }

    if (showFriendRemoveConfirm && state.user != null) {
        FriendRemoveConfirmDialog(
            userName = state.user?.name,
            status = state.friendStatus,
            working = state.friendWorking,
            onConfirm = {
                viewModel.removeFriend()
                showFriendRemoveConfirm = false
            },
            onDismiss = { showFriendRemoveConfirm = false }
        )
    }
}

/** تأكيد إلغاء طلب الصداقة / إزالة صديق / رفض طلب وارد. */
@Composable
private fun FriendRemoveConfirmDialog(
    userName: String?,
    status: FriendStatus,
    working: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val name = userName ?: S.get(R.string.label_this_user)
    val (title, message, confirmLabel) = when (status) {
        FriendStatus.PENDING_SENT -> Triple(
            S.get(R.string.friend_cancel_request_title),
            S.get(R.string.friend_cancel_confirm, name),
            S.get(R.string.friend_cancel_request)
        )
        FriendStatus.PENDING_RECEIVED -> Triple(
            S.get(R.string.friend_decline_request_title),
            S.get(R.string.friend_decline_confirm, name),
            S.get(R.string.action_decline)
        )
        else -> Triple(
            S.get(R.string.friends_remove_title),
            S.get(R.string.friends_remove_confirm, name),
            S.get(R.string.action_remove)
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.PersonRemove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm, enabled = !working) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(S.get(R.string.action_undo)) }
        }
    )
}

@Composable
private fun BlockConfirmDialog(
    userName: String?,
    blocking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                Icons.Filled.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = S.get(R.string.chat_block_user),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = S.get(R.string.chat_block_confirm_named, userName ?: S.get(R.string.label_this_user)),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm, enabled = !blocking) {
                Text(S.get(R.string.action_block), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(S.get(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun ProfileContent(
    user: UserProfile,
    currentUserPremium: Boolean,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    onLike: () -> Unit,
    onSuperLike: () -> Unit,
    onSkip: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onOpenPhoto: (Int) -> Unit,
    liked: Boolean,
    blocked: Boolean,
    reported: Boolean,
    friendStatus: FriendStatus,
    friendWorking: Boolean,
    onAddFriend: () -> Unit,
    onAcceptFriend: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    // حساب موقوف بالكامل → بيانات مقنّعة, نخفي كل أزرار التفاعل والأمان
    val suspended = user.isSuspendedAccount == true

    // بوابة زر الرسالة حسب إعدادات الطرف الآخر
    val messageBlockedReason: String? = when {
        user.acceptingRequests == false -> S.get(R.string.profile_no_new_requests)
        user.premiumOnlyRequests == true && !currentUserPremium -> S.get(R.string.profile_premium_only_requests)
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            HeroSection(user = user, onOpenPhoto = { onOpenPhoto(0) })

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IntroCard(user)
                PhotosSection(user, onOpenPhoto = onOpenPhoto)

                if (!suspended) {
                    messageBlockedReason?.let { reason ->
                        Text(
                            text = "ℹ️ $reason",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        // الإبلاغ/الحظر في قائمة ⋯ أعلى الشاشة (مثل iOS) بدل بطاقة أسفل الملف
        UserTopBar(
            onBack = onBack,
            showMenu = !suspended,
            reported = reported,
            blocked = blocked,
            onReport = onReport,
            onBlock = onBlock,
            onUnblock = onUnblock
        )

        // الحساب الموقوف لا يملك أزرار تفاعل (رسالة/إعجاب/مميز/تخطي)
        if (!suspended) {
            UserActionBar(
                onSkip = onSkip,
                onSuperLike = onSuperLike,
                onLike = onLike,
                onMessage = onMessage,
                liked = liked,
                messageEnabled = messageBlockedReason == null,
                friendStatus = friendStatus,
                friendWorking = friendWorking,
                onFriendClick = {
                    when (friendStatus) {
                        FriendStatus.NONE -> onAddFriend()
                        FriendStatus.PENDING_RECEIVED -> onAcceptFriend()
                        else -> onRemoveFriend()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}



// ── حالة الاتصال الذكية: متصل خلال ساعتين = «متصل»، وإلا «غير متصل» ──
private val profileIsoParser by lazy {
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
        isLenient = true
    }
}

private fun minutesSince(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    val trimmed = iso.substringBefore('.').trimEnd('Z')
    val date = runCatching { profileIsoParser.parse(trimmed) }.getOrNull() ?: return null
    return ((System.currentTimeMillis() - date.time) / 60_000).coerceAtLeast(0)
}

/** @return (نص الحالة, نشِط؟) — نشِط = أخضر. */
internal fun connectionStatus(isOnline: Boolean?, lastLogin: String?): Pair<String, Boolean> {
    if (isOnline == true) return S.get(R.string.status_online_now) to true
    val mins = minutesSince(lastLogin)
    return if (mins != null && mins <= 120) S.get(R.string.status_online) to true else S.get(R.string.status_offline) to false
}










// ──────────────────────────────────────────────────
// Skeleton أثناء التحميل
// ──────────────────────────────────────────────────
@Composable
private fun ProfileSkeleton(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            com.chathala.hala.ui.components.SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(460.dp),
                shape = RoundedCornerShape(0.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    com.chathala.hala.ui.components.SkeletonBlock(
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }
        }
        UserTopBar(onBack = onBack)
    }
}

// ──────────────────────────────────────────────────
// Hero: immersive photo + name + pills
// ──────────────────────────────────────────────────
@Composable
private fun HeroSection(user: UserProfile, onOpenPhoto: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .clickable(onClick = onOpenPhoto)
    ) {
        // الصورة الرئيسية كخلفية غامرة
        val mainPhoto = user.galleryUrls.firstOrNull()
        HalaAsyncImage(
            model = mainPhoto,
            contentDescription = S.get(R.string.photo_of_named, user.name ?: S.get(R.string.label_user)),
            contentScale = ContentScale.Crop,
            fallbackName = user.name,
            modifier = Modifier.fillMaxSize()
        )

        // تدرّج داكن لقراءة النص
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // المعلومات في الأسفل
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (user.likedYou == true) LikedYouBadge()
            NameRow(user)
            HeroPillsRow(user)
        }
    }
}

/** شارة اهتمام متبادل: «أعجب بك». */
@Composable
private fun LikedYouBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFFE91E63))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(S.get(R.string.profile_liked_you), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NameRow(user: UserProfile) {
    val age = ProfileFormatter.computeAge(user.birthDate)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (age != null) {
            Text(
                text = "$age",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (user.verification?.isVerified == true) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = Color(0xFF2EA9FF),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = user.name ?: S.get(R.string.label_user),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        if (user.isPremium == true) {
            Text(text = "👑", fontSize = 20.sp)
        }
    }
}





















// ──────────────────────────────────────────────────
// Message bottom sheet (reuses quick messages row)
// ──────────────────────────────────────────────────
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MessageBottomSheet(
    targetName: String?,
    sending: Boolean,
    alreadySent: Boolean,
    onSend: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = S.get(R.string.message_to_named, targetName ?: S.get(R.string.label_user)),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            QuickMessagesRow(
                onPick = { text = it },
                enabled = !sending && !alreadySent
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(S.get(R.string.discover_write_message)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 4
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text(S.get(R.string.action_cancel)) }

                androidx.compose.material3.Button(
                    onClick = { onSend(text.trim().ifBlank { null }) },
                    enabled = !sending && !alreadySent,
                    modifier = Modifier.weight(1f)
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(if (alreadySent) S.get(R.string.label_sent_check) else S.get(R.string.action_send), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * حالة "حساب محذوف" — يظهر عند فتح ملف مستخدم حذف حسابه (ردّ الخادم 404).
 * بلا زر إعادة محاولة: الوثيقة محذوفة نهائياً من الخادم.
 */
@Composable
private fun DeletedAccountState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = S.get(R.string.deleted_account_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = S.get(R.string.profile_account_deleted_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        androidx.compose.material3.OutlinedButton(onClick = onBack) {
            Text(S.get(R.string.action_back))
        }
    }
}
