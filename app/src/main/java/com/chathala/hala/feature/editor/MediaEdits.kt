package com.chathala.hala.feature.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

/** نسب القصّ الجاهزة. */
enum class CropAspect(val ratio: Float?, val label: String) {
    FREE(null, ""), SQUARE(1f, "1:1"), PORTRAIT(4f / 5f, "4:5"), STORY(9f / 16f, "9:16"), WIDE(16f / 9f, "16:9")
}

/**
 * ملصق إيموجي. الموضع (مركزه) نسبيّ 0..1 من إطار الوسيط، والحجم نسبةً من عرضه —
 * فيُرسم على الأصل بالدقة نفسها التي يُعرض بها على المعاينة المصغّرة.
 */
data class Sticker(
    val id: Long,
    val emoji: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    /** عرض الملصق نسبةً من عرض الإطار. */
    val size: Float = 0.22f,
    val rotation: Float = 0f,
    /** ملصق صورة: رابط ملصق اللوحة أو مفتاح `local:` لشارة مرسومة على الجهاز (StickerImageStore) */
    val imageKey: String? = null,
    /** أشكال الشارة — الضغط على الملصق المحدَّد ينتقل بينها */
    val variants: List<String> = emptyList(),
    /** ملصق المدينة: الاسم فقط (لا إحداثيات) — يُرسل locationTag مع القصة */
    val locationPlace: String? = null,
    /** العرض ÷ الارتفاع لملصق الصورة (الشارات أعرض من طولها) */
    val aspect: Float = 1f
)

/** تعديلات صورة واحدة (لكل صورة تعديلاتها في وضع تعدّد الصور). */
data class PhotoEdits(
    val rotation: Int = 0,
    val aspect: CropAspect = CropAspect.FREE,
    val stickers: List<Sticker> = emptyList(),
    val blurFaces: Boolean = false,
    /** −1…1 */
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    /** صورة مؤقتة (محادثة فقط): null = عادية. */
    val timerSec: Int? = null
) {
    val isEdited: Boolean
        get() = rotation != 0 || aspect != CropAspect.FREE || stickers.isNotEmpty() || blurFaces ||
            brightness != 0f || contrast != 0f || saturation != 0f || timerSec != null
}

data class VideoEdits(
    val startMs: Long = 0,
    val endMs: Long = 15_000,
    val mute: Boolean = false,
    val stickers: List<Sticker> = emptyList(),
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f
)

/** مصفوفة لون تقريبية لسطوع/تباين/تشبّع في −1…1 (المعاينة والصورة النهائية). */
fun colorMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
    val m = ColorMatrix()
    m.setSaturation(1f + saturation)
    val c = 1f + contrast
    val t = (1f - c) * 128f + brightness * 100f
    m.postConcat(
        ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return m
}

object PhotoRenderer {

    /** دوران + قصّ مركزي + ألوان (بدون الملصقات والتمويه). */
    fun base(src: Bitmap, edits: PhotoEdits): Bitmap {
        var bmp = src
        if (edits.rotation % 360 != 0) {
            val m = Matrix().apply { postRotate(edits.rotation.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        edits.aspect.ratio?.let { ratio ->
            val current = bmp.width.toFloat() / bmp.height
            bmp = if (current > ratio) {
                val w = (bmp.height * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createBitmap(bmp, (bmp.width - w) / 2, 0, w, bmp.height)
            } else {
                val h = (bmp.width / ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createBitmap(bmp, 0, (bmp.height - h) / 2, bmp.width, h)
            }
        }
        if (edits.brightness != 0f || edits.contrast != 0f || edits.saturation != 0f) {
            val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(bmp, 0f, 0f, Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix(edits.brightness, edits.contrast, edits.saturation))
            })
            bmp = out
        }
        return bmp
    }

    /** الصورة النهائية: base + تمويه الوجوه + الملصقات — على الأصل لا المعاينة. */
    suspend fun render(src: Bitmap, edits: PhotoEdits): Bitmap = withContext(Dispatchers.Default) {
        var bmp = base(src, edits)
        if (!bmp.isMutable) bmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
        if (edits.blurFaces) FaceBlur.apply(bmp)
        drawStickers(Canvas(bmp), bmp.width, bmp.height, edits.stickers)
        bmp
    }

    fun drawStickers(canvas: Canvas, width: Int, height: Int, stickers: List<Sticker>) {
        if (stickers.isEmpty()) return
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        stickers.forEach { s ->
            val px = s.size * width
            // ملصق صورة (لوحة التحكم / شارة المدينة والحالة): بعرض size وارتفاع حسب نسبته
            val image = StickerImageStore.get(s.imageKey)
            if (s.imageKey != null) {
                if (image != null) {
                    val h = px * image.height / image.width.coerceAtLeast(1)
                    canvas.save()
                    canvas.translate(s.x * width, s.y * height)
                    canvas.rotate(s.rotation)
                    canvas.drawBitmap(image, null, android.graphics.RectF(-px / 2, -h / 2, px / 2, h / 2), bmpPaint)
                    canvas.restore()
                }
                return@forEach
            }
            paint.textSize = px * 0.86f
            val bounds = Rect()
            paint.getTextBounds(s.emoji, 0, s.emoji.length, bounds)
            canvas.save()
            canvas.translate(s.x * width, s.y * height)
            canvas.rotate(s.rotation)
            canvas.drawText(s.emoji, -bounds.exactCenterX(), -bounds.exactCenterY(), paint)
            canvas.restore()
        }
    }

    /** طبقة ملصقات شفافة بأبعاد الإطار — للفيديو (OverlayEffect). */
    fun stickerOverlay(width: Int, height: Int, stickers: List<Sticker>): Bitmap? {
        if (stickers.isEmpty()) return null
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawStickers(Canvas(bmp), width, height, stickers)
        return bmp
    }
}

/** تمويه الوجوه (ML Kit Face Detection): بكسلة كل وجه مكتشف. */
object FaceBlur {
    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.05f)
                .build()
        )
    }

    suspend fun detect(bitmap: Bitmap): List<Rect> = suspendCancellableCoroutine { cont ->
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces -> if (cont.isActive) cont.resume(faces.map { it.boundingBox }) }
            .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) }
    }

    /** يبكسل الوجوه في [bitmap] (قابلة للتعديل) ويُرجع عددها. */
    suspend fun apply(bitmap: Bitmap): Int {
        val faces = detect(bitmap)
        val canvas = Canvas(bitmap)
        faces.forEach { r ->
            // هامش حول الوجه كي لا تبقى الأطراف ظاهرة
            val padX = (r.width() * 0.15f).roundToInt()
            val padY = (r.height() * 0.2f).roundToInt()
            val rect = Rect(
                (r.left - padX).coerceAtLeast(0), (r.top - padY).coerceAtLeast(0),
                (r.right + padX).coerceAtMost(bitmap.width), (r.bottom + padY).coerceAtMost(bitmap.height)
            )
            if (rect.width() < 4 || rect.height() < 4) return@forEach
            val region = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
            val block = max(6, max(rect.width(), rect.height()) / 12)
            val small = Bitmap.createScaledBitmap(region, (rect.width() / block).coerceAtLeast(1), (rect.height() / block).coerceAtLeast(1), true)
            val pixel = Bitmap.createScaledBitmap(small, rect.width(), rect.height(), false)
            canvas.drawBitmap(pixel, rect.left.toFloat(), rect.top.toFloat(), null)
        }
        return faces.size
    }
}
