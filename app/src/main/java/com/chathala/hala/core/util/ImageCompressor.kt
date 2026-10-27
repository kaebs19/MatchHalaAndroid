package com.chathala.hala.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * ضغط الصور قبل الرفع (قصص وصور المحادثة): JPEG ≤ 1MB بجودة ثابتة 0.82،
 * ونصغّر الأبعاد (من 2048 نزولاً بـ 0.8) بدل خفض الجودة حتى 0.1 كما كان في iOS.
 * لا HEIC/WebP — لوحة التحكم وsharp على الخادم تتوقّع JPEG.
 */
object ImageCompressor {

    const val MAX_BYTES = 1_000_000
    const val QUALITY = 82
    const val START_EDGE = 2048
    private const val STEP = 0.8f
    private const val MIN_EDGE = 320

    /** يفكّ الصورة مع احترام اتجاه EXIF (ImageDecoder يطبّقه) ويقصرها على [maxEdge]. */
    suspend fun decode(context: Context, uri: Uri, maxEdge: Int = START_EDGE): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    val w = info.size.width
                    val h = info.size.height
                    val longest = max(w, h)
                    if (longest > maxEdge) {
                        val s = maxEdge.toFloat() / longest
                        decoder.setTargetSize((w * s).roundToInt().coerceAtLeast(1), (h * s).roundToInt().coerceAtLeast(1))
                    }
                }
            }.getOrNull()
        }

    suspend fun compress(context: Context, uri: Uri, maxBytes: Int = MAX_BYTES): ByteArray? {
        val bitmap = decode(context, uri) ?: return null
        return compress(bitmap, maxBytes)
    }

    suspend fun compress(bitmap: Bitmap, maxBytes: Int = MAX_BYTES): ByteArray =
        withContext(Dispatchers.Default) {
            var current = bitmap.limitEdge(START_EDGE)
            while (true) {
                val bytes = ByteArrayOutputStream().use { out ->
                    current.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                    out.toByteArray()
                }
                val longest = max(current.width, current.height)
                if (bytes.size <= maxBytes || longest <= MIN_EDGE) return@withContext bytes
                current = current.limitEdge((longest * STEP).roundToInt())
            }
            @Suppress("UNREACHABLE_CODE") ByteArray(0)
        }

    private fun Bitmap.limitEdge(edge: Int): Bitmap {
        val longest = max(width, height)
        if (longest <= edge) return this
        val s = edge.toFloat() / longest
        return Bitmap.createScaledBitmap(this, (width * s).roundToInt().coerceAtLeast(1), (height * s).roundToInt().coerceAtLeast(1), true)
    }
}
