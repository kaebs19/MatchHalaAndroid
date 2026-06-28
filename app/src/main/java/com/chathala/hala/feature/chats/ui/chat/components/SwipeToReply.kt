package com.chathala.hala.feature.chats.ui.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * يغلّف فقاعة رسالة بإيماءة «السحب للردّ» — مثل واتساب/تيليجرام.
 * عند سحب المحتوى أفقياً لتجاوز العتبة يظهر اهتزاز خفيف، وعند الإفلات يُفعَّل الردّ.
 */
@Composable
fun SwipeToReply(
    onReply: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) { content(); return }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 56.dp.toPx() }
    val maxDrag = with(density) { 80.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var triggered by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // أيقونة الردّ خلف المحتوى — تظهر تدريجياً مع السحب
        val progress = (abs(offsetX.value) / threshold).coerceIn(0f, 1f)
        if (progress > 0.02f) {
            val align = if (offsetX.value > 0) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier
                    .align(align)
                    .padding(horizontal = 14.dp)
                    .size(34.dp)
                    .alpha(progress)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { triggered = false },
                        onDragEnd = {
                            if (abs(offsetX.value) > threshold) onReply()
                            scope.launch { offsetX.animateTo(0f) }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val target = (offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)
                                offsetX.snapTo(target)
                            }
                            if (!triggered && abs(offsetX.value) > threshold) {
                                triggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
