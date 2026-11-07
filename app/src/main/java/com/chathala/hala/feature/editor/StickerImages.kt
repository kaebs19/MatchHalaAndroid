package com.chathala.hala.feature.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.chathala.hala.R
import com.chathala.hala.core.i18n.LocaleManager
import com.chathala.hala.core.i18n.S
import java.util.concurrent.ConcurrentHashMap

/**
 * صور الملصقات في الذاكرة: ملصقات اللوحة (بالرابط) والشارات المرسومة على الجهاز (`local:`).
 * الرسم على الأصل متزامن، فكل ملصق صورة يُحمَّل هنا قبل ظهوره في المحرّر.
 */
object StickerImageStore {
    private val images = ConcurrentHashMap<String, Bitmap>()
    fun get(key: String?): Bitmap? = key?.let { images[it] }
    fun put(key: String, bitmap: Bitmap) { images[key] = bitmap }

    /** يحمّل ملصق اللوحة بدقة كافية للرسم على الأصل (≤2048). */
    suspend fun load(context: Context, url: String): Bitmap? {
        images[url]?.let { return it }
        val request = ImageRequest.Builder(context).data(url).size(512).allowHardware(false).build()
        val bmp = runCatching { context.imageLoader.execute(request).drawable?.toBitmap() }.getOrNull() ?: return null
        images[url] = bmp
        return bmp
    }
}

/** حالة «حالتي» — صيغ ثابتة لا تتغيّر بالجنس، فلا تحتاج فلتر النصّ. */
data class StoryMood(val emoji: String, val labelRes: Int)

val StoryMoods = listOf(
    StoryMood("😊", R.string.mood_happy), StoryMood("🤩", R.string.mood_excited),
    StoryMood("😍", R.string.mood_in_love), StoryMood("🥺", R.string.mood_missing),
    StoryMood("😌", R.string.mood_relaxed), StoryMood("😴", R.string.mood_sleepy),
    StoryMood("😢", R.string.mood_sad), StoryMood("😤", R.string.mood_annoyed),
    StoryMood("🤒", R.string.mood_tired), StoryMood("☕️", R.string.mood_coffee),
    StoryMood("💼", R.string.mood_work), StoryMood("✈️", R.string.mood_travel),
    StoryMood("🏋️", R.string.mood_workout), StoryMood("🎮", R.string.mood_gaming),
    StoryMood("🎧", R.string.mood_music)
)

/**
 * شارتا «📍 المدينة» و«حالتي» (مقابل `StoryInfoStickers` في iOS): تُرسم صوراً بأربعة أشكال
 * (متدرّج، فاتح، داكن، بلا خلفية) — الضغط على الملصق ينتقل بينها.
 */
object InfoStickers {
    private enum class Style { GRADIENT, LIGHT, DARK, PLAIN }
    private const val H = 168f        // ارتفاع الشارة بالبكسل (×3 لدقّة الرسم)
    private const val PAD = 24f

    private fun textColor(s: Style) = if (s == Style.LIGHT) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()

    private fun capsule(canvas: Canvas, w: Float, s: Style) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        when (s) {
            Style.GRADIENT -> p.shader = LinearGradient(0f, 0f, w, 0f,
                intArrayOf(0xFFFF9A3C.toInt(), 0xFFF2307E.toInt(), 0xFFB02BD6.toInt()), null, Shader.TileMode.CLAMP)
            Style.LIGHT -> p.color = 0xFFFFFFFF.toInt()
            Style.DARK -> p.color = 0xB8000000.toInt()
            Style.PLAIN -> return
        }
        canvas.drawRoundRect(RectF(0f, 0f, w, H), H / 2, H / 2, p)
    }

    private fun boldPaint(sizePx: Float, color: Int, plain: Boolean) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        this.color = color
        if (plain) setShadowLayer(9f, 0f, 3f, 0x99000000.toInt())
    }

    /** يرسم الشارة ويعيد (المفتاح، نسبة العرض للارتفاع). */
    private fun draw(iconText: String, title: String?, label: String, style: Style, iconCircle: Int?): Pair<String, Float> {
        val rtl = LocaleManager.isRtl
        val labelPaint = boldPaint(66f, textColor(style), style == Style.PLAIN)
        val titlePaint = title?.let { boldPaint(39f, textColor(style), style == Style.PLAIN).apply { alpha = 215; typeface = Typeface.DEFAULT } }
        val maxLabel = 620f
        val shownLabel = TextUtils.ellipsize(label, labelPaint, maxLabel, TextUtils.TruncateAt.END).toString()
        val textW = maxOf(labelPaint.measureText(shownLabel), titlePaint?.measureText(title) ?: 0f)
        val icon = H - PAD * 2
        val w = PAD + icon + 30f + textW + PAD * 2.5f
        val bmp = Bitmap.createBitmap(w.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        capsule(c, w, style)
        // الأيقونة على البداية (يمين في العربية)
        val iconLeft = if (rtl) w - PAD - icon else PAD
        iconCircle?.let { col ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col }
            if (style == Style.LIGHT && iconText == "📍") {
                p.shader = LinearGradient(iconLeft, 0f, iconLeft + icon, icon,
                    intArrayOf(0xFFFF9A3C.toInt(), 0xFFF2307E.toInt(), 0xFFB02BD6.toInt()), null, Shader.TileMode.CLAMP)
            }
            c.drawCircle(iconLeft + icon / 2, H / 2, icon / 2, p)
        }
        val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = icon * 0.58f; textAlign = Paint.Align.CENTER }
        val fm = emojiPaint.fontMetrics
        c.drawText(iconText, iconLeft + icon / 2, H / 2 - (fm.ascent + fm.descent) / 2, emojiPaint)
        // النص
        val textStart = if (rtl) iconLeft - 30f else iconLeft + icon + 30f
        labelPaint.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        titlePaint?.textAlign = labelPaint.textAlign
        if (titlePaint != null) {
            c.drawText(title!!, textStart, H / 2 - 10f, titlePaint)
            c.drawText(shownLabel, textStart, H / 2 + 56f, labelPaint)
        } else {
            val lm = labelPaint.fontMetrics
            c.drawText(shownLabel, textStart, H / 2 - (lm.ascent + lm.descent) / 2, labelPaint)
        }
        val key = "local:${System.nanoTime()}"
        StickerImageStore.put(key, bmp)
        return key to (w / H)
    }

    /** أشكال ملصق المدينة: المفاتيح + نسبة الأبعاد لكلٍّ. */
    fun location(place: String): List<Pair<String, Float>> =
        listOf(Style.LIGHT, Style.GRADIENT, Style.DARK, Style.PLAIN).map { s ->
            draw("📍", null, place, s, if (s == Style.LIGHT) 0xFFF2307E.toInt() else null)
        }

    fun mood(mood: StoryMood): List<Pair<String, Float>> =
        listOf(Style.GRADIENT, Style.LIGHT, Style.DARK, Style.PLAIN).map { s ->
            draw(mood.emoji, S.get(R.string.mood_title), S.get(mood.labelRes), s,
                if (s == Style.LIGHT) 0xFFF2F2F7.toInt() else 0xFFFFFFFF.toInt())
        }
}

/** ✍️ نصّ على صورة القصة (هلا+): يُرسم صورةً بلون مختار وظلّ خفيف، حتى 80 حرفاً. */
object TextSticker {
    const val MAX_LENGTH = 80
    val colors = listOf(0xFFFFFFFF.toInt(), 0xFFFFD60A.toInt(), 0xFFFF375F.toInt(), 0xFF5AC8FA.toInt(), 0xFF30D158.toInt())

    fun render(text: String, color: Int): String {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 84f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.color = color
            setShadowLayer(10f, 0f, 3f, 0xAA000000.toInt())
        }
        val maxW = 960
        val width = minOf(maxW, (text.lines().maxOf { paint.measureText(it) } + 40).toInt()).coerceAtLeast(60)
        val layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, width - 20)
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        val bmp = Bitmap.createBitmap(width, layout.height + 30, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.translate(10f, 15f)
        layout.draw(c)
        val key = "local:${System.nanoTime()}"
        StickerImageStore.put(key, bmp)
        return key
    }
}
