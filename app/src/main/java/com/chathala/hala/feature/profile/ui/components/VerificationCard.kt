package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Verified
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

/**
 * بطاقة حالة التوثيق.
 * status: "verified" | "pending" | "rejected" | "none" (أو null)
 */
@Composable
fun VerificationCard(
    isVerified: Boolean,
    status: String?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (icon, title, subtitle, accent) = verificationStyle(isVerified, status)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(icon = icon, accent = accent)
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Style(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accent: Color
)

private fun verificationStyle(isVerified: Boolean, status: String?): Style = when {
    isVerified || status == "verified" -> Style(
        icon = Icons.Filled.Verified,
        title = "موثّق",
        subtitle = "تم التحقق من حسابك بنجاح",
        accent = Color(0xFF34C759)
    )
    status == "pending" -> Style(
        icon = Icons.Filled.HourglassTop,
        title = "قيد المراجعة",
        subtitle = "طلب التوثيق قيد المراجعة من الإدارة",
        accent = Color(0xFFFF9500)
    )
    status == "rejected" -> Style(
        icon = Icons.Filled.GppMaybe,
        title = "مرفوض",
        subtitle = "لم يتم قبول طلب التوثيق — حاول مرة أخرى",
        accent = Color(0xFFFF3B30)
    )
    else -> Style(
        icon = Icons.Filled.GppGood,
        title = "غير موثّق",
        subtitle = "وثّق حسابك لبناء الثقة مع الآخرين",
        accent = Color(0xFF8E8E93)
    )
}

@Composable
private fun IconCircle(icon: ImageVector, accent: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(40.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
    }
}
