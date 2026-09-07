package com.chathala.hala.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.chathala.hala.core.storage.ChatWallpaper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * خلفية المحادثة — نقش مرسوم برمجياً (لا صور) يتلوّن بلون الثيم الأساسي بشفافية
 * خفيفة، فيبقى مقروءاً في الوضعين الفاتح والداكن ولا يزيد حجم التطبيق.
 *
 * يُبنى المسار مرة واحدة لكل مقاس عبر [drawWithCache] فلا تُعاد الحسابات مع كل إطار.
 *
 * @param scale تصغير النقش للمعاينات المصغّرة في الإعدادات (1f = المقاس الطبيعي).
 */
@Composable
fun Modifier.chatWallpaper(
    wallpaper: ChatWallpaper,
    scale: Float = 1f
): Modifier {
    val background = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val cellPx = with(density) { (72.dp * scale).toPx() }
    // على الأسود يحتاج النقش شفافية أعلى قليلاً ليُرى بنفس النعومة
    val dark = background.luminance() < 0.5f
    val fillAlpha = if (dark) 0.14f else 0.10f
    val lineAlpha = if (dark) 0.22f else 0.20f
    return drawWithCache {
        val layers = buildPattern(wallpaper, size.width, size.height, cellPx)
        val strokeWidth = with(density) { (1.3.dp * scale).toPx() }
        // تدرّج عمودي خفيف جداً يمنح العمق ويكسر التسطيح
        val gradient = Brush.verticalGradient(
            0f to background,
            1f to tint.copy(alpha = if (dark) 0.10f else 0.05f).compositeOver(background)
        )
        onDrawBehind {
            drawRect(gradient)
            if (wallpaper == ChatWallpaper.NONE) return@onDrawBehind
            drawPath(layers.fill, tint.copy(alpha = fillAlpha))
            drawPath(layers.outline, tint.copy(alpha = lineAlpha), style = Stroke(width = strokeWidth))
        }
    }
}

/** طبقتان: أشكال ممتلئة وأخرى مفرّغة — المزج يعطي إحساس التصميم لا التكرار. */
private class PatternLayers(val fill: Path, val outline: Path)

// ── بناء المسارات ──

private fun buildPattern(
    wallpaper: ChatWallpaper,
    width: Float,
    height: Float,
    cell: Float
): PatternLayers {
    val fill = Path()
    val outline = Path()
    val layers = PatternLayers(fill, outline)
    if (wallpaper == ChatWallpaper.NONE || width <= 0f || height <= 0f) return layers
    if (wallpaper == ChatWallpaper.WAVES) {
        outline.addPath(wavesPath(width, height, cell))
        return layers
    }

    val cols = (width / cell).toInt() + 2
    val rows = (height / cell).toInt() + 2
    for (row in 0 until rows) {
        val shift = if (row % 2 == 0) 0f else cell / 2f
        for (col in 0 until cols) {
            val r1 = noise(col, row, 1)
            val r2 = noise(col, row, 2)
            val r3 = noise(col, row, 3)
            val r4 = noise(col, row, 4)

            if (wallpaper == ChatWallpaper.DOTS) {
                // نقاط منتظمة صغيرة بلا فجوات — نقش هادئ ومتّزن
                fill.addOval(
                    androidx.compose.ui.geometry.Rect(
                        Offset(col * cell + shift, row * cell + cell / 2f), cell * 0.055f
                    )
                )
                continue
            }

            // فجوات عشوائية (~28%) تكسر إحساس الشبكة
            if (r4 < 0.28f) continue

            val cx = col * cell + shift + (r1 - 0.5f) * cell * 0.55f
            val cy = row * cell + cell / 2f + (r2 - 0.5f) * cell * 0.55f
            // أحجام متباينة: أغلبها صغير وبعضها أكبر بوضوح
            val size = cell * if (r3 < 0.7f) (0.14f + r3 * 0.12f) else (0.24f + (r3 - 0.7f) * 0.5f)
            val rotation = (r1 - 0.5f) * 70f
            // ثلث الأشكال مفرّغة والباقي ممتلئة
            val target = if (r2 < 0.34f) outline else fill

            when (wallpaper) {
                ChatWallpaper.HEARTS -> target.addTransformed(heartPath(size), cx, cy, rotation)
                ChatWallpaper.STARS -> target.addTransformed(starPath(size), cx, cy, rotation)
                ChatWallpaper.BUBBLES -> {
                    val radius = size * (0.7f + r2 * 0.6f)
                    outline.addOval(androidx.compose.ui.geometry.Rect(Offset(cx, cy), radius))
                    // لمعة صغيرة داخل الفقاعات الأكبر
                    if (radius > cell * 0.22f) {
                        fill.addOval(
                            androidx.compose.ui.geometry.Rect(
                                Offset(cx - radius * 0.35f, cy - radius * 0.35f), radius * 0.16f
                            )
                        )
                    }
                }
                else -> Unit
            }
        }
    }
    return layers
}

/** قلب بحجم [s] مركزه في الأصل (0,0). */
private fun heartPath(s: Float): Path {
    val w = s
    val h = s * 0.92f
    val x = -w / 2f
    val y = -h / 2f
    return Path().apply {
        moveTo(x + w / 2f, y + h / 5f)
        cubicTo(x + 5f * w / 14f, y, x, y + h / 15f, x + w / 28f, y + 2f * h / 5f)
        cubicTo(x + w / 14f, y + 2f * h / 3f, x + 3f * w / 7f, y + 5f * h / 6f, x + w / 2f, y + h)
        cubicTo(x + 4f * w / 7f, y + 5f * h / 6f, x + 13f * w / 14f, y + 2f * h / 3f, x + 27f * w / 28f, y + 2f * h / 5f)
        cubicTo(x + w, y + h / 15f, x + 9f * w / 14f, y, x + w / 2f, y + h / 5f)
        close()
    }
}

/** نجمة خماسية بقطر [s] مركزها في الأصل. */
private fun starPath(s: Float): Path {
    val outer = s / 2f
    val inner = outer * 0.45f
    return Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val px = (r * cos(angle)).toFloat()
            val py = (r * sin(angle)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}

/** خطوط جيبية متوازية تمتدّ بعرض الشاشة. */
private fun wavesPath(width: Float, height: Float, cell: Float): Path {
    val path = Path()
    val gap = cell * 0.75f
    val amplitude = cell * 0.12f
    val wavelength = cell * 1.6f
    var y = gap / 2f
    var i = 0
    while (y < height + amplitude) {
        val phase = if (i % 2 == 0) 0f else (PI).toFloat()
        path.moveTo(0f, y)
        var x = 0f
        val step = 6f
        while (x <= width + step) {
            val yy = y + amplitude * sin(2f * PI.toFloat() * x / wavelength + phase)
            path.lineTo(x, yy)
            x += step
        }
        y += gap
        i++
    }
    return path
}

private fun Path.addTransformed(shape: Path, cx: Float, cy: Float, rotationDeg: Float) {
    val m = Matrix()
    m.translate(cx, cy)
    m.rotateZ(rotationDeg)
    shape.transform(m)
    addPath(shape)
}

/** عشوائية حتمية 0..1 — يبقى النقش ثابتاً بين الإطارات وإعادة التركيب. */
private fun noise(x: Int, y: Int, salt: Int): Float {
    var h = x * 374761393 + y * 668265263 + salt * 982451653
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return ((h and 0x7fffffff) % 10_000) / 10_000f
}
