package com.chathala.hala.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged

/**
 * Placeholder متحرّك (shimmer effect).
 * يستخدم أثناء تحميل البيانات من السيرفر.
 *
 * مثال:
 * ```
 * SkeletonBlock(
 *     modifier = Modifier.size(140.dp).clip(CircleShape)
 * )
 * ```
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    // قيمة الإزاحة 0→1 لتحريك بقعة اللمعان أفقياً عبر العنصر
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton-progress"
    )

    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    var widthPx by remember { mutableFloatStateOf(0f) }

    // بقعة لمعان متحرّكة تعبر العنصر من جهة لأخرى (shimmer متموّج)
    val sweep = widthPx * 1.6f
    val startX = -sweep + progress * (widthPx + sweep)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(startX, 0f),
        end = androidx.compose.ui.geometry.Offset(startX + sweep, 0f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .background(if (widthPx > 0f) brush else Brush.horizontalGradient(listOf(base, base)))
    )
}
