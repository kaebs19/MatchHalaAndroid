package com.chathala.hala.feature.userprofile.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chathala.hala.R
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.util.HapticHelper
import com.chathala.hala.core.util.ProfileFormatter
import com.chathala.hala.core.util.Zodiac
import com.chathala.hala.feature.friends.data.FriendStatus
import com.chathala.hala.feature.profile.ui.components.ReadOnlyInterestsChips
import com.chathala.hala.feature.userprofile.data.UserProfile

// ألوان دلالية ثابتة (مطابقة لألوان iOS النظامية) — لا تتبع الثيم عمداً.
private val Green = Color(0xFF34C759)
private val Pink = Color(0xFFFF2D55)
private val Blue = Color(0xFF2E9BFF)
private val Cyan = Color(0xFF32ADE6)
private val Red = Color(0xFFFF3B30)
private val Orange = Color(0xFFFF9500)
private val Purple = Color(0xFFAF52DE)
private val Gold = Color(0xFFFFC107)

// ══════════════════════════════════════════════════
// الشريط العلوي: رجوع + قائمة ⋯ (إبلاغ/حظر) — دوائر زجاجية فوق الصورة
// ══════════════════════════════════════════════════

@Composable
internal fun UserTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showMenu: Boolean = false,
    reported: Boolean = false,
    blocked: Boolean = false,
    onReport: () -> Unit = {},
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, S.get(R.string.action_back), onBack)
        if (showMenu) {
            Box {
                GlassIconButton(Icons.Filled.MoreVert, null) { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (reported) S.get(R.string.discover_reported) else S.get(R.string.chat_report_user),
                                color = MaterialTheme.colorScheme.error.copy(alpha = if (reported) 0.5f else 1f)
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Flag, null, tint = MaterialTheme.colorScheme.error) },
                        enabled = !reported,
                        onClick = { menuOpen = false; onReport() }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (blocked) S.get(R.string.profile_unblock_user) else S.get(R.string.chat_block_user),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; if (blocked) onUnblock() else onBlock() }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

// ══════════════════════════════════════════════════
// رقائق الهيرو فوق الصورة: حضور • جنس • توثيق • دولة • برج • ميلاد • بريميوم
// ══════════════════════════════════════════════════

@Composable
internal fun HeroPillsRow(user: UserProfile) {
    val (statusText, active) = connectionStatus(user.isOnline, user.lastLogin)
    // الرقائق الوصفية بلون محايد (أبيض فوق الصورة) — الملوّن فقط ما له دلالة: الحضور والتوثيق
    val neutral = Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (user.isSuspendedAccount != true) {
            val presence = if (active) Green else Color(0xFFB0B0B8)
            HeroPill(text = statusText, tint = presence, dot = presence)
        }
        if (user.verification?.isVerified == true) {
            HeroPill(icon = Icons.Filled.Verified, text = S.get(R.string.verify_verified), tint = Blue)
        }
        when (user.gender) {
            "female" -> HeroPill(icon = Icons.Filled.Female, text = S.get(R.string.gender_female_short), tint = neutral)
            "male" -> HeroPill(icon = Icons.Filled.Male, text = S.get(R.string.gender_male_short), tint = neutral)
        }
        Countries.byCode(user.country)?.let {
            HeroPill(emoji = it.flag, text = it.name, tint = neutral)
        }
        Zodiac.fromBirthDate(user.birthDate)?.let { z ->
            HeroPill(emoji = z.emoji, text = z.name, tint = neutral)
        }
        Zodiac.birthdayLabel(user.birthDate)?.let { bd ->
            HeroPill(emoji = "🎈", text = bd, tint = neutral)
        }
        if (user.isPremium == true) {
            HeroPill(icon = Icons.Filled.WorkspacePremium, text = S.get(R.string.profile_premium_user), tint = Gold)
        }
    }
}

@Composable
private fun HeroPill(
    text: String,
    tint: Color,
    icon: ImageVector? = null,
    emoji: String? = null,
    dot: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .border(0.5.dp, tint.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (dot != null) Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
        if (emoji != null) Text(emoji, fontSize = 12.sp)
        Text(text, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ══════════════════════════════════════════════════
// بطاقة التعريف: النبذة + الاهتمامات + شرائح معلومات (عضو منذ/المسافة)
// ══════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IntroCard(user: UserProfile) {
    val bio = user.bio?.takeIf { it.isNotBlank() }
    val hasInterests = user.interests.isNotEmpty()

    ProfileCard {
        if (bio == null && !hasInterests) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = S.get(R.string.profile_no_details_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = S.get(R.string.profile_no_details_subtitle),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                InfoChipsRow(user)
            }
            return@ProfileCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (bio != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(Icons.Filled.FormatQuote, S.get(R.string.profile_bio))
                    Text(text = bio, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                }
                SoftDivider()
            }
            InfoChipsRow(user)
            if (hasInterests) {
                SoftDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(Icons.Filled.AutoAwesome, S.get(R.string.profile_interests), trailing = user.interests.size.toString())
                    ReadOnlyInterestsChips(interestKeys = user.interests)
                }
            }
        }
    }
}

@Composable
private fun SoftDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .size(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoChipsRow(user: UserProfile) {
    val age = ProfileFormatter.computeAge(user.birthDate)
    val country = Countries.byCode(user.country)
    val memberSince = ProfileFormatter.formatJoinDate(user.joinDate)
    val distance = user.distance
    // الحضور يظهر في رقيقة الهيرو — لا نكرّره هنا
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (age != null) InfoChip(Icons.Filled.Cake, S.get(R.string.profile_age_years_value, age), tint = null)
        if (country != null) InfoChip(Icons.Filled.Place, country.name, tint = null)
        if (memberSince != null) {
            InfoChip(Icons.Filled.Schedule, "${S.get(R.string.label_member_since)} $memberSince", tint = null)
        }
        if (distance != null) {
            InfoChip(Icons.Filled.LocationOn, S.get(R.string.distance_km_bullet, distance.toInt()), tint = Green)
        }
    }
}

/** شريحة معلومة: لون دلالي إن كان له معنى (حضور/مسافة)، وإلا محايدة. */
@Composable
private fun InfoChip(icon: ImageVector, text: String, tint: Color?) {
    val fg = tint ?: MaterialTheme.colorScheme.onSurface
    val bg = tint?.copy(alpha = 0.12f) ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ══════════════════════════════════════════════════
// الصور
// ══════════════════════════════════════════════════

@Composable
internal fun PhotosSection(user: UserProfile, onOpenPhoto: (Int) -> Unit) {
    val urls = user.galleryUrls.drop(1) // الأولى هي صورة الهيرو
    if (urls.isEmpty()) return
    ProfileCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(Icons.Filled.PhotoLibrary, S.get(R.string.profile_photos), trailing = urls.size.toString())
            urls.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { colIndex, url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onOpenPhoto(rowIndex * 2 + colIndex + 1) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════
// عناصر مشتركة
// ══════════════════════════════════════════════════

/** بطاقة موحّدة: سطح، زوايا 16، حشو 20 — كل الأقسام بالنمط نفسه. */
@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(22.dp)
    ) { content() }
}

/** رأس قسم: أيقونة بلون التطبيق + عنوان — لا دوائر ممتلئة ولا رموز تعبيرية. */
@Composable
private fun SectionHeader(icon: ImageVector, title: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (trailing != null) {
            Text(trailing, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ══════════════════════════════════════════════════
// شريط الإجراءات السفلي (نمط Snap): دائرة 50 بخلفية خفيفة وأيقونة ملوّنة
// ══════════════════════════════════════════════════

@Composable
internal fun UserActionBar(
    onSkip: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit,
    onMessage: () -> Unit,
    liked: Boolean,
    messageEnabled: Boolean,
    friendStatus: FriendStatus,
    friendWorking: Boolean,
    onFriendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val likeScale by animateFloatAsState(
        targetValue = if (liked) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "likeScale"
    )
    val (friendLabel, friendIcon, friendTint, friendFilled) = when (friendStatus) {
        FriendStatus.FRIENDS -> FriendLook(S.get(R.string.label_friend), Icons.Filled.HowToReg, Green, true)
        FriendStatus.PENDING_SENT -> FriendLook(S.get(R.string.label_request_sent), Icons.Filled.HourglassTop, Orange, false)
        FriendStatus.PENDING_RECEIVED -> FriendLook(S.get(R.string.action_accept), Icons.Filled.PersonAdd, Green, false)
        FriendStatus.NONE -> FriendLook(S.get(R.string.action_add), Icons.Filled.PersonAdd, Purple, false)
    }
    val shape = RoundedCornerShape(28.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SnapActionButton(S.get(R.string.action_message), Icons.AutoMirrored.Filled.Chat, MaterialTheme.colorScheme.primary,
            enabled = messageEnabled, modifier = Modifier.weight(1f)) { HapticHelper.light(haptic); onMessage() }
        SnapActionButton(friendLabel, friendIcon, friendTint, filled = friendFilled, loading = friendWorking,
            modifier = Modifier.weight(1f)) { HapticHelper.medium(haptic); onFriendClick() }
        SnapActionButton(S.get(R.string.action_like), if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            if (liked) Pink else Green, filled = liked, scale = likeScale, modifier = Modifier.weight(1f)) { HapticHelper.medium(haptic); onLike() }
        SnapActionButton(S.get(R.string.label_premium), Icons.Filled.Star, Cyan, modifier = Modifier.weight(1f)) { HapticHelper.medium(haptic); onSuperLike() }
        SnapActionButton(S.get(R.string.action_skip), Icons.Filled.Close, Red, modifier = Modifier.weight(1f)) { HapticHelper.light(haptic); onSkip() }
    }
}

private data class FriendLook(val label: String, val icon: ImageVector, val tint: Color, val filled: Boolean)

@Composable
private fun SnapActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    scale: Float = 1f,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .then(if (filled) Modifier.shadow(6.dp, CircleShape, ambientColor = tint.copy(alpha = 0.4f), spotColor = tint.copy(alpha = 0.4f)) else Modifier)
                .clip(CircleShape)
                .background(if (filled) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                .border(0.8.dp, if (filled) tint.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
        ) {
            if (loading) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = if (filled) Color.White else tint, modifier = Modifier.size(18.dp))
            } else {
                Icon(icon, label, tint = if (filled) Color.White else tint, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}
