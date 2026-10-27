package com.chathala.hala.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * طبقة الملصقات فوق الوسيط: سحب، إطار تحديد، مقبض زاوية للتكبير والتدوير بإصبع
 * واحد، وقرص بإصبعين، و✕ للحذف.
 *
 * ⚠️ التخطيط **من اليسار لليمين دائماً** (في RTL انعكست المواقع في iOS)،
 * والإيماءات على الملصق نفسه لا على الطبقة كلها.
 */
@Composable
fun StickerLayer(
    stickers: List<Sticker>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onChange: (Sticker) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            stickers.forEach { s ->
                key(s.id) {
                    StickerItem(
                        sticker = s,
                        frameW = wPx,
                        frameH = hPx,
                        selected = s.id == selectedId,
                        density = density.density,
                        onSelect = { onSelect(s.id) },
                        onChange = onChange,
                        onDelete = { onDelete(s.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun key(id: Long, content: @Composable () -> Unit) = androidx.compose.runtime.key(id) { content() }

@Composable
private fun StickerItem(
    sticker: Sticker,
    frameW: Float,
    frameH: Float,
    selected: Boolean,
    density: Float,
    onSelect: () -> Unit,
    onChange: (Sticker) -> Unit,
    onDelete: () -> Unit
) {
    val current by rememberUpdatedState(sticker)
    val sizePx = sticker.size * frameW
    val sizeDp = (sizePx / density).dp
    val left = (sticker.x * frameW - sizePx / 2f).roundToInt()
    val top = (sticker.y * frameH - sizePx / 2f).roundToInt()

    Box(
        Modifier
            .absoluteOffset { IntOffset(left, top) }
            .size(sizeDp)
            .rotate(sticker.rotation)
            .then(if (selected) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(6.dp)) else Modifier)
            .pointerInput(sticker.id) {
                detectTapGestures(onTap = { onSelect() })
            }
            .pointerInput(sticker.id) {
                // سحب بإصبع + قرص/تدوير بإصبعين
                detectTransformGestures { _, pan, zoom, rotation ->
                    onSelect()
                    val s = current
                    val rad = Math.toRadians(s.rotation.toDouble())
                    // pan بإحداثيات الملصق المدوَّر ← نعيده لإحداثيات الإطار
                    val dx = (pan.x * Math.cos(rad) - pan.y * Math.sin(rad)).toFloat()
                    val dy = (pan.x * Math.sin(rad) + pan.y * Math.cos(rad)).toFloat()
                    onChange(
                        s.copy(
                            x = (s.x + dx / frameW).coerceIn(0f, 1f),
                            y = (s.y + dy / frameH).coerceIn(0f, 1f),
                            size = (s.size * zoom).coerceIn(0.06f, 0.9f),
                            rotation = s.rotation + rotation
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(sticker.emoji, fontSize = with(androidx.compose.ui.unit.Density(density)) { (sizePx * 0.72f).toSp() })
        if (selected) {
            // ✕ أعلى-يسار
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .absoluteOffset((-10).dp, (-10).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .pointerInput(sticker.id) { detectTapGestures(onTap = { onDelete() }) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            // مقبض الزاوية: تكبير وتدوير بإصبع واحد حول مركز الملصق
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .absoluteOffset(10.dp, 10.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput(sticker.id) {
                        // نجمع متجه (المركز ← الزاوية) في إحداثيات الإطار: الإيماءات تصل بإحداثيات
                        // الملصق المدوَّر، فنعيد تدوير كل إزاحة بالزاوية الحالية
                        var v0 = Offset.Zero
                        var v = Offset.Zero
                        var startSize = 0f
                        var startRot = 0f
                        fun Offset.rotated(deg: Float): Offset {
                            val r = Math.toRadians(deg.toDouble())
                            val c = Math.cos(r).toFloat(); val sn = Math.sin(r).toFloat()
                            return Offset(x * c - y * sn, x * sn + y * c)
                        }
                        detectDragGestures(
                            onDragStart = {
                                val s = current
                                val half = s.size * frameW / 2f
                                v0 = Offset(half, half).rotated(s.rotation)
                                v = v0
                                startSize = s.size
                                startRot = s.rotation
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val s = current
                                v += drag.rotated(s.rotation)
                                val base = hypot(v0.x, v0.y).coerceAtLeast(1f)
                                val angle = Math.toDegrees((atan2(v.y, v.x) - atan2(v0.y, v0.x)).toDouble()).toFloat()
                                onChange(
                                    s.copy(
                                        size = (startSize * (hypot(v.x, v.y) / base)).coerceIn(0.06f, 0.9f),
                                        rotation = startRot + angle
                                    )
                                )
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.OpenInFull, null, tint = Color.Black, modifier = Modifier.size(13.dp)) }
        }
    }
}
