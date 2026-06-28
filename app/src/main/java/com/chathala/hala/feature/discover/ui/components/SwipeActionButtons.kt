package com.chathala.hala.feature.discover.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chathala.hala.core.util.HapticHelper

/**
 * صف أزرار التحكم بأسفل البطاقة (مطابق iOS ExploreView):
 *  ↩️ Undo (بريميوم) | ✕ Skip | 💬 Message | ⭐ Super Like (بريميوم) | ❤️ Like
 */
@Composable
fun SwipeActionButtons(
    onSkip: () -> Unit,
    onMessage: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit,
    onUndo: () -> Unit,
    isPremium: Boolean,
    isLiked: Boolean,
    canUndo: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            size = 44.dp,
            iconSize = 18.dp,
            color = Color(0xFF1DD1B1),
            onClick = { HapticHelper.light(haptic); onUndo() },
            contentLabel = "تراجع",
            showCrown = !isPremium,
            enabled = canUndo || !isPremium,
            isFilled = false
        )
        ActionButton(
            icon = Icons.Filled.Close,
            size = 56.dp,
            iconSize = 24.dp,
            color = Color(0xFFFF5A5F),
            onClick = { HapticHelper.medium(haptic); onSkip() },
            contentLabel = "تخطي",
            isFilled = false
        )
        ActionButton(
            icon = Icons.AutoMirrored.Filled.Chat,
            size = 50.dp,
            iconSize = 20.dp,
            color = MaterialTheme.colorScheme.primary,
            onClick = { HapticHelper.light(haptic); onMessage() },
            contentLabel = "إرسال رسالة",
            isFilled = false
        )
        ActionButton(
            icon = Icons.Filled.Star,
            size = 50.dp,
            iconSize = 20.dp,
            color = Color(0xFF2EA9FF),
            onClick = { HapticHelper.medium(haptic); onSuperLike() },
            contentLabel = "إعجاب مميز",
            showCrown = !isPremium,
            isFilled = false
        )
        ActionButton(
            icon = Icons.Filled.Favorite,
            size = 56.dp,
            iconSize = 24.dp,
            color = if (isLiked) Color(0xFFE91E63) else Color(0xFF4CAF50),
            onClick = { HapticHelper.medium(haptic); onLike() },
            contentLabel = "إعجاب",
            isFilled = isLiked
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    size: Dp,
    iconSize: Dp,
    color: Color,
    onClick: () -> Unit,
    contentLabel: String,
    isFilled: Boolean = false,
    showCrown: Boolean = false,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "actionButtonScale"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 8.dp)
            .scale(scale)
            .semantics { contentDescription = contentLabel }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = if (isFilled) Brush.linearGradient(
                        listOf(color, color.copy(alpha = 0.7f))
                    ) else Brush.linearGradient(
                        listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.08f))
                    )
                )
                .border(
                    width = if (isFilled) 0.dp else 1.5.dp,
                    color = color.copy(alpha = 0.55f),
                    shape = CircleShape
                )
                .alpha(if (enabled) 1f else 0.35f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) {
                    pressed = true
                    onClick()
                    pressed = false
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFilled) Color.White else color,
                modifier = Modifier.size(iconSize)
            )
        }
        if (showCrown) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
