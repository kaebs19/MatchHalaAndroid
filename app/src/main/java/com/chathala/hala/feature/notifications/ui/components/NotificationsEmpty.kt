package com.chathala.hala.feature.notifications.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chathala.hala.R
import com.chathala.hala.feature.notifications.ui.NotificationFilter

@Composable
fun NotificationsEmpty(filter: NotificationFilter) {
    val isDone = filter == NotificationFilter.UNREAD
    val icon: ImageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.NotificationsNone
    val title = if (isDone)
        stringResource(R.string.notifications_empty_unread)
    else
        stringResource(R.string.notifications_empty_title)
    val subtitle = stringResource(R.string.notifications_empty_subtitle)

    // Entry animation
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(filter) {
        entered = false
        entered = true
    }
    val enterScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "empty-enter-scale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "empty-enter-alpha"
    )

    // Idle breathe (scale ping-pong) للـ icon
    val breathe = rememberInfiniteTransition(label = "breathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe-scale"
    )

    // Rotation خفيف (wobble) إذا done
    val wobble = rememberInfiniteTransition(label = "wobble")
    val wobbleDeg by wobble.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble-deg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .alpha(enterAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IllustrationHalo(
            icon = icon,
            tint = if (isDone)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.primary,
            scale = enterScale * breatheScale,
            rotation = if (isDone) 0f else wobbleDeg
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IllustrationHalo(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    scale: Float,
    rotation: Float
) {
    // طبقات halo دائرية متدرجة
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // halo خارجي
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale * 0.98f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            tint.copy(alpha = 0.18f),
                            tint.copy(alpha = 0f)
                        )
                    )
                )
        )
        // halo وسط
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            tint.copy(alpha = 0.28f),
                            tint.copy(alpha = 0.08f)
                        )
                    )
                )
        )
        // دائرة الأيقونة
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(scale)
                .rotate(rotation)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            tint,
                            tint.copy(alpha = 0.72f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
