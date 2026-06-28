package com.chathala.hala.feature.chats.ui.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.feature.chats.ui.list.ChatsFilter

@Composable
fun ChatsFilterChips(
    selected: ChatsFilter,
    unreadCount: Int,
    pendingCount: Int,
    pinnedCount: Int,
    premiumCount: Int,
    onSelect: (ChatsFilter) -> Unit,
    onOpenRequests: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            label = "الكل",
            selected = selected == ChatsFilter.ALL,
            onClick = { onSelect(ChatsFilter.ALL) }
        )
        FilterChip(
            label = "غير مقروءة",
            count = unreadCount,
            icon = Icons.Filled.Email,
            selected = selected == ChatsFilter.UNREAD,
            onClick = { onSelect(ChatsFilter.UNREAD) }
        )
        FilterChip(
            label = "طلبات",
            count = pendingCount,
            isAccent = true,
            selected = false,
            onClick = onOpenRequests
        )
        FilterChip(
            label = "مقربون",
            count = pinnedCount,
            icon = Icons.Filled.PushPin,
            iconTint = Color(0xFFFFB300),
            selected = selected == ChatsFilter.PINNED,
            onClick = { onSelect(ChatsFilter.PINNED) }
        )
        FilterChip(
            label = "مميزون",
            count = premiumCount,
            icon = Icons.Filled.Star,
            iconTint = Color(0xFFFFB300),
            selected = selected == ChatsFilter.PREMIUM,
            onClick = { onSelect(ChatsFilter.PREMIUM) }
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int = 0,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    isAccent: Boolean = false,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        isAccent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val fg = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isAccent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: fg,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            fontSize = 13.sp
        )
        if (count > 0) {
            Spacer(Modifier.size(2.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.primary
                    )
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}
