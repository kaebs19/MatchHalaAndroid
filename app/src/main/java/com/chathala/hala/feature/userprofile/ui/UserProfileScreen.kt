package com.chathala.hala.feature.userprofile.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
                reported = state.reported
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
                text = "حظر المستخدم",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "هل أنت متأكد من حظر ${userName ?: "هذا المستخدم"}؟ لن يتمكن من مراسلتك أو رؤية ملفك.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm, enabled = !blocking) {
                Text("حظر", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("إلغاء") }
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
    reported: Boolean
) {
    // حساب موقوف بالكامل → بيانات مقنّعة، نخفي كل أزرار التفاعل والأمان
    val suspended = user.isSuspendedAccount == true

    // بوابة زر الرسالة حسب إعدادات الطرف الآخر
    val messageBlockedReason: String? = when {
        user.acceptingRequests == false -> "لا يستقبل طلبات جديدة"
        user.premiumOnlyRequests == true && !currentUserPremium -> "يستقبل من المشتركين فقط"
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
                BasicInfoCard(user)
                LocationCard(user)
                BioCard(user)
                PhotosCard(user, onOpenPhoto = onOpenPhoto)

                if (!suspended) {
                    messageBlockedReason?.let { reason ->
                        Text(
                            text = "ℹ️ $reason",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    SafetyActions(
                        blocked = blocked,
                        reported = reported,
                        onBlock = onBlock,
                        onUnblock = onUnblock,
                        onReport = onReport
                    )
                }
            }
        }

        TopBar(onBack = onBack)

        // الحساب الموقوف لا يملك أزرار تفاعل (رسالة/إعجاب/مميز/تخطي)
        if (!suspended) {
            FloatingActionBar(
                onSkip = onSkip,
                onSuperLike = onSuperLike,
                onLike = onLike,
                onMessage = onMessage,
                liked = liked,
                messageEnabled = messageBlockedReason == null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            )
        }
    }
}

/** بطاقة المعلومات الأساسية + حالة الاتصال. */
@Composable
private fun BasicInfoCard(user: UserProfile) {
    val age = ProfileFormatter.computeAge(user.birthDate)
    val genderText = when (user.gender) {
        "male" -> "ذكر"
        "female" -> "أنثى"
        else -> null
    }
    val (statusText, active) = connectionStatus(user.isOnline, user.lastLogin)
    val statusColor = if (active) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
    val memberSince = ProfileFormatter.formatJoinDate(user.joinDate)

    InfoCard(
        icon = { Text("👤", fontSize = 18.sp) },
        title = "معلومات أساسية"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoLine("الحالة", statusText, valueColor = statusColor)
            genderText?.let { InfoLine("الجنس", it) }
            age?.let { InfoLine("العمر", "$it سنة") }
            memberSince?.let { InfoLine("عضو منذ", it) }
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
private fun connectionStatus(isOnline: Boolean?, lastLogin: String?): Pair<String, Boolean> {
    if (isOnline == true) return "متصل الآن" to true
    val mins = minutesSince(lastLogin)
    return if (mins != null && mins <= 120) "متصل" to true else "غير متصل" to false
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.width(64.dp)
        )
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** نصوص الأمان (حظر/إلغاء حظر/إبلاغ) أسفل الملف. */
@Composable
private fun SafetyActions(
    blocked: Boolean,
    reported: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SafetyRow(
            icon = Icons.Filled.Flag,
            text = if (reported) "✓ تم الإبلاغ" else "الإبلاغ عن المستخدم",
            enabled = !reported,
            onClick = onReport
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        )
        // محظور → «إلغاء الحظر»؛ غير محظور → «حظر المستخدم» (بتأكيد)
        SafetyRow(
            icon = Icons.Filled.Block,
            text = if (blocked) "إلغاء حظر المستخدم" else "حظر المستخدم",
            enabled = true,
            onClick = if (blocked) onUnblock else onBlock
        )
    }
}

@Composable
private fun SafetyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 1f else 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ──────────────────────────────────────────────────
// Top bar
// ──────────────────────────────────────────────────
@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White
            )
        }
    }
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
        TopBar(onBack = onBack)
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
        if (mainPhoto != null) {
            AsyncImage(
                model = mainPhoto,
                contentDescription = "صورة ${user.name ?: "المستخدم"}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        }

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
            PillsRow(user)
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
        Text("أعجب بك", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            text = user.name ?: "مستخدم",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        if (user.isPremium == true) {
            Text(text = "👑", fontSize = 20.sp)
        }
    }
}

@Composable
private fun PillsRow(user: UserProfile) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (user.verification?.isVerified == true) {
            Pill(icon = "✓", text = "موثّق", tint = Color(0xFF2EA9FF))
        }
        if (user.isOnline == true) {
            OnlinePill()
        }
        Zodiac.fromBirthDate(user.birthDate)?.let { z ->
            Pill(icon = z.emoji, text = z.nameAr, tint = Color(0xFFB388FF))
        }
        Zodiac.birthdayLabel(user.birthDate)?.let { bd ->
            Pill(icon = "🎈", text = bd, tint = Color(0xFFFF5A5F))
        }
        if (user.isPremium == true) {
            Pill(icon = "👑", text = "مستخدم مميز", tint = Color(0xFFFFC107))
        }
    }
}

@Composable
private fun Pill(icon: String, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, tint.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = icon, fontSize = 13.sp)
        Text(text = text, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OnlinePill() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Text("متصل", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ──────────────────────────────────────────────────
// Cards: location / bio / photos
// ──────────────────────────────────────────────────
@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) { icon() }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun LocationCard(user: UserProfile) {
    val country = Countries.byCode(user.country)
    if (country == null && user.distance == null) return
    InfoCard(
        icon = {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        },
        title = "الموقع"
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            country?.let {
                Text(text = it.flag, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = it.nameAr,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }
            user.distance?.let {
                if (country != null) Spacer(Modifier.width(10.dp))
                Text(
                    text = "• ${it.toInt()} كم",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BioCard(user: UserProfile) {
    val bio = user.bio?.takeIf { it.isNotBlank() } ?: return
    InfoCard(
        icon = {
            Text("📝", fontSize = 18.sp)
        },
        title = "نبذة عني"
    ) {
        Text(
            text = bio,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PhotosCard(user: UserProfile, onOpenPhoto: (Int) -> Unit) {
    val urls = user.galleryUrls.drop(1) // first one is profile already shown above
    if (urls.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📸", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "الصور",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${urls.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(((urls.size + 1) / 2 * 180).dp)
        ) {
            itemsIndexed(urls) { index, url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        // index في الشبكة = index+1 في galleryUrls (الأولى صورة الهيرو)
                        .clickable { onOpenPhoto(index + 1) }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────
// Floating action bar
// ──────────────────────────────────────────────────
@Composable
private fun FloatingActionBar(
    onSkip: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit,
    onMessage: () -> Unit,
    liked: Boolean,
    messageEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    // نبضة القلب عند الإعجاب
    val likeScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (liked) 1.18f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "likeScale"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileActionButton("تخطي", Icons.Filled.Close, Color(0xFFFF5A5F)) {
            HapticHelper.medium(haptic); onSkip()
        }
        ProfileActionButton("مميز", Icons.Filled.Star, Color(0xFF2EA9FF)) {
            HapticHelper.medium(haptic); onSuperLike()
        }
        ProfileActionButton(
            label = "إعجاب",
            icon = Icons.Filled.Favorite,
            color = if (liked) Color(0xFFE91E63) else Color(0xFF4CAF50),
            filled = liked,
            scale = likeScale
        ) {
            HapticHelper.medium(haptic); onLike()
        }
        ProfileActionButton(
            label = "رسالة",
            icon = Icons.AutoMirrored.Filled.Chat,
            color = Color(0xFFE91E8C),
            enabled = messageEnabled
        ) {
            HapticHelper.light(haptic); onMessage()
        }
    }
}

@Composable
private fun ProfileActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    filled: Boolean = false,
    scale: Float = 1f,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    brush = if (filled) Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))
                            else Brush.linearGradient(listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.1f)))
                )
                .border(1.5.dp, color.copy(alpha = if (filled) 0f else 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (filled) Color.White else color,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
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
                text = "رسالة إلى ${targetName ?: "المستخدم"}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            QuickMessagesRow(
                onSend = { onSend(it) },
                enabled = !sending && !alreadySent
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("اكتب رسالتك…") },
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
                ) { Text("إلغاء") }

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
                        Text(if (alreadySent) "✓ تم الإرسال" else "إرسال", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
