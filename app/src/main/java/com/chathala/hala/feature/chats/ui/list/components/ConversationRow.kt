package com.chathala.hala.feature.chats.ui.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chathala.hala.feature.chats.data.Conversation
import com.chathala.hala.feature.notifications.util.NotificationFormat

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ConversationRow(
    conversation: Conversation,
    currentUserId: String?,
    isMuted: Boolean = false,
    isPinned: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val other = conversation.participants.firstOrNull { it.id != currentUserId }
        ?: conversation.participants.firstOrNull()
    val lastMessage = conversation.lastMessage
    val unread = conversation.unreadCount
    val isOnline = other?.isOnline == true
    val isRecentlyActive = !isOnline && isRecentLogin(other?.lastLogin)
    val isPremium = other?.isPremium == true
    val isVerified = other?.verification?.isVerified == true

    Row(
        modifier = modifier
            .fillMaxWidth()
            // خلفية صلبة — تمنع ظهور خلفية السحب (delete/pin) عند الراحة
            .background(
                if (isPinned)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                else
                    MaterialTheme.colorScheme.background
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPinned) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.size(8.dp))
        }

        RingAvatar(
            imageUrl = other?.profileImage,
            name = other?.name,
            isOnline = isOnline,
            isRecentlyActive = isRecentlyActive,
            isPremium = isPremium
        )

        Spacer(Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // الصف الأعلى: الاسم + الوقت
            Row(verticalAlignment = Alignment.CenterVertically) {
                // مجموعة الاسم + التوثيق تأخذ كل المساحة المرنة وتدفع الوقت لأقصى الحافة
                // (الثبات: الوقت يلتصق دائماً بنهاية الصف بدل أن يتدرّج حسب طول الاسم)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = other?.name ?: "مستخدم",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
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
                Spacer(Modifier.size(8.dp))
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                }
                if (isMuted) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                }
                val time = lastMessage?.createdAt?.let(NotificationFormat::timeAgoArabic)
                    ?: presenceLabel(isOnline, isRecentlyActive)
                if (!time.isNullOrBlank()) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unread > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // الصف السفلي: ✓✓ + preview + badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                val senderId = lastMessage?.sender
                if (senderId != null && currentUserId != null && senderId == currentUserId) {
                    ReadStatusIcon(isRead = lastMessage.isRead == true)
                    Spacer(Modifier.size(4.dp))
                }
                Text(
                    text = previewText(lastMessage?.type, lastMessage?.content, conversation),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (unread > 0)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (unread > 0) {
                    Spacer(Modifier.size(8.dp))
                    UnreadBadge(count = unread)
                }
            }
        }
    }
}

@Composable
private fun RingAvatar(
    imageUrl: String?,
    name: String?,
    isOnline: Boolean,
    isRecentlyActive: Boolean,
    isPremium: Boolean
) {
    val ringColor = when {
        isOnline -> Color(0xFF22C55E)       // أخضر: متصل الآن (أولوية أعلى)
        isPremium -> Color(0xFFFFB300)      // ذهبي: بريميوم (غير متصل)
        isRecentlyActive -> Color(0xFFFF9500)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier.size(58.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .border(width = 2.dp, color = ringColor, shape = CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
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
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
            }
        } else if (isRecentlyActive) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9500))
                )
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ReadStatusIcon(isRead: Boolean) {
    Icon(
        imageVector = if (isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
        contentDescription = null,
        tint = if (isRead)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp)
    )
}

private fun previewText(
    type: String?,
    content: String?,
    conv: Conversation
): String = when (type) {
    "image" -> "📷 صورة"
    "audio" -> "🎙️ رسالة صوتية"
    "video" -> "🎥 فيديو"
    "file" -> "📎 ملف"
    "system" -> "— تحديث المحادثة"
    else -> content?.takeIf { it.isNotBlank() }
        ?: conv.initialMessage?.content?.takeIf { it.isNotBlank() }
        ?: "ابدأ المحادثة 👋"
}

private fun presenceLabel(isOnline: Boolean, recent: Boolean): String? = when {
    isOnline -> "متصل"
    recent -> "نشط مؤخراً"
    else -> null
}

/** اعتبار "نشط مؤخراً" خلال آخر 24 ساعة. */
private fun isRecentLogin(lastLoginIso: String?): Boolean {
    if (lastLoginIso.isNullOrBlank()) return false
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val cleaned = lastLoginIso.substringBefore(".").substringBefore("Z")
        val d = fmt.parse(cleaned) ?: return false
        (System.currentTimeMillis() - d.time) < 24L * 60 * 60 * 1000
    } catch (_: Throwable) {
        false
    }
}
