package com.chathala.hala.feature.chats.ui.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * موجة الرسالة الصوتية — نظير [VoiceWaveformView] في iOS بنفس المقاسات والسلوك.
 * الأعمدة تُعاد أخذ عيّناتها لتملأ العرض المتاح مهما كان طول التسجيل،
 * والضغط أو السحب الأفقي ينقل موضع التشغيل.
 *
 * ملاحظة RTL: الموجة تمثّل زمناً لا نصّاً، فترتيب الأعمدة يبقى من اليسار
 * لليمين، لكن إحداثيات اللمس تُعكس عند العرض في اتجاه RTL.
 */
@Composable
fun VoiceWaveform(
    levels: List<Float>,
    progress: Float,
    playedColor: Color,
    remainingColor: Color,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val seek = rememberUpdatedState(onSeek)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "waveformProgress"
    )

    val seekModifier = if (onSeek != null) {
        Modifier
            .pointerInput(isRtl) {
                detectTapGestures { offset ->
                    seek.value?.invoke(fraction(offset.x, size.width.toFloat(), isRtl))
                }
            }
            .pointerInput(isRtl) {
                // السحب الأفقي فقط — العمودي يبقى لتمرير قائمة الرسائل
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    seek.value?.invoke(fraction(change.position.x, size.width.toFloat(), isRtl))
                }
            }
    } else Modifier

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(MAX_BAR_HEIGHT.dp)
            .then(seekModifier)
    ) {
        val barWidthPx = BAR_WIDTH.dp.toPx()
        val spacingPx = BAR_SPACING.dp.toPx()
        val count = max(1, ((size.width + spacingPx) / (barWidthPx + spacingPx)).toInt())
        val bars = resample(levels, count)
        val maxHeightPx = MAX_BAR_HEIGHT.dp.toPx()
        val minHeightPx = MIN_BAR_HEIGHT.dp.toPx()

        bars.forEachIndexed { index, level ->
            val position = if (count > 1) index.toFloat() / (count - 1) else 0f
            val played = position <= animatedProgress
            val barHeight = max(minHeightPx, level.coerceIn(0f, 1f) * maxHeightPx)
            val left = if (isRtl) {
                size.width - (index + 1) * barWidthPx - index * spacingPx
            } else {
                index * (barWidthPx + spacingPx)
            }
            drawBar(
                left = left,
                width = barWidthPx,
                height = barHeight,
                color = if (played) playedColor else remainingColor
            )
        }
    }
}

private fun DrawScope.drawBar(left: Float, width: Float, height: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(left, (size.height - height) / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2f, width / 2f)
    )
}

private fun fraction(x: Float, width: Float, isRtl: Boolean): Float {
    if (width <= 0f) return 0f
    val raw = (x / width).coerceIn(0f, 1f)
    return if (isRtl) 1f - raw else raw
}

/**
 * إعادة أخذ العيّنات لتملأ عدد الأعمدة المتاح — الاقتصار على آخر N عيّنة
 * كان سيُظهر آخر ثوانٍ فقط من تسجيل طويل.
 */
private fun resample(levels: List<Float>, count: Int): List<Float> {
    if (levels.isEmpty() || count <= 0) return List(max(count, 1)) { FLAT_LEVEL }
    if (levels.size <= count) {
        // تسجيل قصير: وزّع القيم على كامل العرض بدل موجة مضغوطة في الطرف
        return (0 until count).map { levels[it * levels.size / count] }
    }
    val bucket = levels.size.toDouble() / count
    return (0 until count).map { index ->
        val start = (index * bucket).toInt()
        val end = minOf(levels.size, max(start + 1, ((index + 1) * bucket).toInt()))
        levels.subList(start, end).max()
    }
}

/** موجة بديلة ثابتة لكل رسالة حين لا يرسل الطرف الآخر waveform حقيقياً. */
@Composable
fun rememberFallbackWaveform(seed: String): List<Float> = remember(seed) {
    val hash = seed.hashCode()
    (0 until FALLBACK_BARS).map { i ->
        val normalized = abs((hash + i * 31) % 100) / 100f
        0.2f + normalized * 0.7f
    }
}

private const val BAR_WIDTH = 2.5f
private const val BAR_SPACING = 2f
private const val MAX_BAR_HEIGHT = 26f
private const val MIN_BAR_HEIGHT = 4f
private const val FLAT_LEVEL = 0.25f
private const val FALLBACK_BARS = 40
