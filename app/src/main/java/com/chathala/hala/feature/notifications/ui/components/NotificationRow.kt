package com.chathala.hala.feature.notifications.ui.components

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.chathala.hala.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chathala.hala.feature.notifications.data.NotificationItem
import com.chathala.hala.feature.notifications.util.NotificationFormat
import com.chathala.hala.ui.components.HalaAsyncImage

@Composable
fun NotificationRow(
    item: NotificationItem,
    currentUserId: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    friendResult: Boolean? = null,
    friendBusy: Boolean = false,
    onFriendRespond: ((accept: Boolean) -> Unit)? = null
) {
    val isRead = NotificationFormat.isReadByCurrentUser(item, currentUserId)
    val haptics = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // تصميم مبسّط ومسطّح: كل الصفوف بلون البطاقة نفسه — الجديد يُميَّز بنقطة صغيرة فقط
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (isPressed) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            NotificationLeading(item = item)

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                    if (isOfficial(item)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = S.get(R.string.notif_official_account),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    val title = item.title?.takeIf { it.isNotBlank() } ?: typeTitle(item.type)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isRead) FontWeight.Medium else FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val body = item.body
                    if (!body.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3
                        )
                    }
                    val image = item.image?.takeIf { it.isNotBlank() }
                    if (image != null && isOfficial(item)) {
                        Spacer(Modifier.height(8.dp))
                        HalaAsyncImage(
                            model = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    // طلب صداقة: قبول/رفض في الصفّ — الزرّان يستهلكان الضغطة فلا تفتح الملف
                    if (item.type == "friend_request" && item.data?.get("friendshipId") is String && onFriendRespond != null) {
                        Spacer(Modifier.height(10.dp))
                        FriendRequestActions(friendResult, friendBusy, onFriendRespond)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = NotificationFormat.timeAgo(item.createdAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val groupCount = item.groupCount
                        if (groupCount != null && groupCount > 1) {
                            Spacer(Modifier.size(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "×$groupCount",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                if (!isRead) {
                    Spacer(Modifier.size(8.dp))
                    UnreadDot()
                }
        }
    }
}

/** نقطة صغيرة تُميّز الإشعار غير المقروء (بلا خلفية ملوّنة). */
@Composable
private fun UnreadDot() {
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun NotificationLeading(item: NotificationItem) {
    // إشعارات النظام/الإدارة → هوية رسمية بدل صورة المُرسِل (الأدمن)
    if (isOfficial(item)) {
        OfficialAvatar()
        return
    }
    val senderImage = item.sender?.profileImage?.takeIf { it.isNotBlank() }
    // مرسِل بلا صورة يستحق حرفه الأول، لا أيقونة نوع الإشعار العامة.
    if (senderImage != null || item.sender?.name != null) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            HalaAsyncImage(
                model = senderImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                fallbackName = item.sender?.name,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
            )
            val badge = typeBadge(item.type)
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(badge.second)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badge.first,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    } else {
        val (icon, color) = typeBadge(item.type)
            ?: (Icons.Filled.Notifications to MaterialTheme.colorScheme.primary)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(
                            color.copy(alpha = 0.25f),
                            color.copy(alpha = 0.10f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * أنواع الإشعارات الرسمية (من النظام/الإدارة) — تُعرض بهوية هلا الرسمية.
 * احتياط لخادم قديم لا يُرسل `isOfficial`؛ `general` هو النوع الافتراضي لإشعار اللوحة.
 */
private val OFFICIAL_TYPES = setOf(
    "account_warning", "account_hidden", "account_restricted", "account_suspended",
    "account_unhidden", "account_unsuspended", "account_banned",
    "banned_word", "report_warning", "restriction", "restriction_lifted", "security_alert",
    "system", "general", "warning", "official_warning", "announcement", "broadcast",
    "verification", "promo_warning", "appeal_update", "maintenance_start", "maintenance_end",
    "story_removed", "story_banned", "story_held"
)

/** إشعارات القصص الرسمية — الضغط يفتح «ملفي» لا بروفايل المرسل. */
fun isStoryNotice(type: String?): Boolean =
    type == "story_removed" || type == "story_banned" || type == "story_held" || type == "story_reaction"

/** إشعار رسمي؟ لا تُستعمل بيانات مرسِله (هو مشرف) في العرض ولا في التنقّل. */
fun isOfficial(item: NotificationItem): Boolean {
    if (item.isOfficial == true || item.sender?.isOfficial == true) return true
    val type = item.type ?: return false
    return type in OFFICIAL_TYPES || type.startsWith("account_")
}

/** أفاتار رسمي: شعار هلا على دائرة بيضاء + شارة توثيق. */
@Composable
private fun OfficialAvatar() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.dardasha_hala_log),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(width = 1.5.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
private fun typeBadge(type: String?): Pair<ImageVector, Color>? = when (type) {
    "like", "new_like" -> Icons.Filled.Favorite to MaterialTheme.colorScheme.error
    "super_like" -> Icons.Filled.Star to MaterialTheme.colorScheme.tertiary
    "match", "new_match" -> Icons.Filled.Favorite to MaterialTheme.colorScheme.primary
    "profile_view" -> Icons.Filled.Visibility to MaterialTheme.colorScheme.secondary
    "new_follower" -> Icons.Filled.Person to MaterialTheme.colorScheme.primary
    "friend_request" -> Icons.Filled.PersonAdd to MaterialTheme.colorScheme.primary
    "friend_accepted" -> Icons.Filled.People to Color(0xFF4CAF50)
    "verification" -> Icons.Filled.Shield to MaterialTheme.colorScheme.primary
    "warning", "official_warning", "account_suspended",
    "account_restricted", "restriction", "security_alert" ->
        Icons.Filled.Shield to MaterialTheme.colorScheme.error
    "announcement", "broadcast", "general", "system" ->
        Icons.Filled.Info to MaterialTheme.colorScheme.primary
    "story_removed", "story_banned", "story_held" ->
        Icons.Filled.AutoStories to Color(0xFFD62976)
    // تفاعل مع قصتي — صورة المتفاعل مع شارة قلب
    "story_reaction" -> Icons.Filled.Favorite to Color(0xFFD62976)
    else -> null
}

private fun typeTitle(type: String?): String = when (type) {
    "like", "new_like" -> S.get(R.string.notif_new_like)
    "super_like" -> S.get(R.string.notif_super_like)
    "match", "new_match" -> S.get(R.string.notif_new_match)
    "profile_view" -> S.get(R.string.notif_profile_visit)
    "new_follower" -> S.get(R.string.notif_new_follower)
    "friend_request" -> S.get(R.string.notif_friend_request)
    "friend_accepted" -> S.get(R.string.notif_friend_added)
    "verification" -> S.get(R.string.notif_account_verification)
    "warning", "official_warning" -> S.get(R.string.notif_admin_alert)
    "account_suspended" -> S.get(R.string.notif_account_suspended)
    "account_restricted", "restriction" -> S.get(R.string.notif_account_restricted)
    "security_alert" -> S.get(R.string.notif_security_alert)
    "announcement", "broadcast" -> S.get(R.string.notif_announcement)
    else -> S.get(R.string.notif_generic)
}

@Composable
private fun FriendRequestActions(result: Boolean?, busy: Boolean, onRespond: (Boolean) -> Unit) {
    if (result != null) {
        Text(
            text = S.get(if (result) R.string.notif_friend_accepted_done else R.string.notif_friend_declined_done),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (result) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Button(
            onClick = { onRespond(true) },
            enabled = !busy,
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) { Text(S.get(R.string.notif_action_accept), style = MaterialTheme.typography.labelLarge) }
        androidx.compose.material3.OutlinedButton(
            onClick = { onRespond(false) },
            enabled = !busy,
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) { Text(S.get(R.string.notif_action_decline), style = MaterialTheme.typography.labelLarge) }
    }
}
