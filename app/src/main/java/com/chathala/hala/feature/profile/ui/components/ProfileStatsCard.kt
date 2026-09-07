package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.feature.profile.data.ProfileStats

/** بطاقة النشاط: أربعة أعمدة (أصدقاء/محادثات/إعجابات/زوّار) تطفو فوق حافة الهيدر. */
@Composable
fun ProfileStatsCard(
    stats: ProfileStats,
    onFriends: () -> Unit,
    onChats: () -> Unit,
    onVisitors: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatColumn(Icons.Filled.People, MaterialTheme.colorScheme.primary, stats.friends, S.get(R.string.profile_stat_friends), onFriends)
        StatDivider()
        StatColumn(Icons.Filled.Forum, Color(0xFF9C27B0), stats.conversations, S.get(R.string.profile_stat_chats), onChats)
        StatDivider()
        StatColumn(Icons.Filled.Favorite, Color(0xFFFF2D55), stats.likes, S.get(R.string.profile_stat_likes), null)
        StatDivider()
        StatColumn(Icons.Filled.Visibility, Color(0xFF00BCD4), stats.visitors, S.get(R.string.profile_stat_visitors), onVisitors)
    }
}

@Composable
private fun RowScope.StatColumn(
    icon: ImageVector,
    color: Color,
    count: Int,
    label: String,
    onClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatCount(count),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
    )
}

/** 1.2K / 1.5M مثل iOS. */
internal fun formatCount(n: Int): String {
    fun short(v: Float, suffix: String) =
        String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0") + suffix
    return when {
        n >= 1_000_000 -> short(n / 1_000_000f, "M")
        n >= 1_000 -> short(n / 1_000f, "K")
        else -> n.toString()
    }
}
