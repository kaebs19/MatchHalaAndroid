package com.chathala.hala.feature.notifications.ui.components

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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

@Composable
fun NotificationRow(
    item: NotificationItem,
    currentUserId: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
                    if (isOfficial(item.type)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = "هلا • حساب رسمي",
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
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = NotificationFormat.timeAgoArabic(item.createdAt),
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
    if (isOfficial(item.type)) {
        OfficialAvatar()
        return
    }
    val senderImage = item.sender?.profileImage?.takeIf { it.isNotBlank() }
    if (senderImage != null) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(senderImage)
                    .crossfade(200)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
                    MaterialTheme.colorScheme.surfaceVariant
                ),
                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Person),
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

/** أنواع الإشعارات الرسمية (من النظام/الإدارة) — تُعرض بهوية هلا الرسمية. */
private val OFFICIAL_TYPES = setOf(
    "account_warning", "account_hidden", "account_restricted", "account_suspended",
    "account_unhidden", "account_unsuspended", "account_banned",
    "banned_word", "report_warning", "restriction", "security_alert",
    "system", "warning", "official_warning", "announcement", "broadcast", "verification"
)

private fun isOfficial(type: String?): Boolean =
    type != null && (type in OFFICIAL_TYPES || type.startsWith("account_"))

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
    "verification" -> Icons.Filled.Shield to MaterialTheme.colorScheme.primary
    "warning", "official_warning", "account_suspended",
    "account_restricted", "restriction", "security_alert" ->
        Icons.Filled.Shield to MaterialTheme.colorScheme.error
    "announcement", "broadcast", "general", "system" ->
        Icons.Filled.Info to MaterialTheme.colorScheme.primary
    else -> null
}

private fun typeTitle(type: String?): String = when (type) {
    "like", "new_like" -> "إعجاب جديد"
    "super_like" -> "سوبر لايك"
    "match", "new_match" -> "مطابقة جديدة"
    "profile_view" -> "زيارة لبروفايلك"
    "new_follower" -> "متابع جديد"
    "verification" -> "تحقق من الحساب"
    "warning", "official_warning" -> "تنبيه من الإدارة"
    "account_suspended" -> "تعليق الحساب"
    "account_restricted", "restriction" -> "قيود على الحساب"
    "security_alert" -> "تنبيه أمني"
    "announcement", "broadcast" -> "إعلان"
    else -> "إشعار"
}
