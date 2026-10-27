package com.chathala.hala.core.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodecInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ضغط فيديو المحادثة على الجهاز (Media3 Transformer):
 * H.264 (HEVC غير مضمون في كل الأجهزة) بأقصر ضلع 720 و~2Mbps ثابت ← 15ث ≈ 3.8MB < 6MB.
 * القصّ لنافذة ≤15ث، الاتجاه محفوظ، والتعديلات (ألوان/ملصقات/كتم) تُطبّق على الأصل.
 */
@OptIn(UnstableApi::class)
object VideoCompressor {

    const val MAX_DURATION_MS = 15_000L
    const val MAX_BYTES = 6L * 1024 * 1024
    private const val SHORT_SIDE = 720
    private const val BITRATE = 2_000_000

    data class Meta(val durationMs: Long, val width: Int, val height: Int)

    data class Edits(
        val startMs: Long = 0,
        val endMs: Long = MAX_DURATION_MS,
        val mute: Boolean = false,
        /** −1…1 */
        val brightness: Float = 0f,
        /** −1…1 */
        val contrast: Float = 0f,
        /** −1…1 */
        val saturation: Float = 0f,
        /** طبقة الملصقات بأبعاد الإطار المعروض (null = لا شيء). */
        val overlay: Bitmap? = null
    )

    data class Output(val file: File, val durationSec: Double, val width: Int, val height: Int)

    /** الأبعاد كما تُعرض (بعد الدوران) والمدة. */
    suspend fun probe(context: Context, uri: Uri): Meta? = withContext(Dispatchers.IO) {
        runCatching {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (rot == 90 || rot == 270) Meta(d, h, w) else Meta(d, w, h)
            }
        }.getOrNull()
    }

    /** إطار مصغّر من الفيديو (لمعاينة شريط القصّ). */
    suspend fun frameAt(context: Context, uri: Uri, timeMs: Long, maxEdge: Int = 160): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                r.getScaledFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxEdge, maxEdge)
            }
        }.getOrNull()
    }

    /** أبعاد الناتج: أقصر ضلع 720 (لا تكبير)، وأرقام زوجية للمرمّز. */
    fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        val short = min(width, height).coerceAtLeast(1)
        val scale = if (short > SHORT_SIDE) SHORT_SIDE.toFloat() / short else 1f
        fun even(v: Float) = (v.roundToInt() / 2 * 2).coerceAtLeast(2)
        return even(width * scale) to even(height * scale)
    }

    /**
     * يضغط [source] إلى ملف في [outDir] مع تقدّم 0..1. قابل للإلغاء (إلغاء الـ coroutine
     * يوقف التصدير ويحذف الملف) — يُعاد الضغط عند تغيّر التعديلات.
     */
    suspend fun compress(
        context: Context,
        source: Uri,
        meta: Meta,
        edits: Edits,
        outDir: File,
        onProgress: (Float) -> Unit
    ): Output = withContext(Dispatchers.Main) {
        outDir.mkdirs()
        val out = File(outDir, "video_${System.currentTimeMillis()}.mp4")
        val start = edits.startMs.coerceIn(0, max(0, meta.durationMs - 1))
        val end = min(edits.endMs, meta.durationMs).coerceAtMost(start + MAX_DURATION_MS).coerceAtLeast(start + 1)
        val (w, h) = targetSize(meta.width, meta.height)

        val item = MediaItem.Builder()
            .setUri(source)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(start)
                    .setEndPositionMs(end)
                    .build()
            )
            .build()

        val effects = mutableListOf<Effect>()
        if (edits.brightness != 0f) effects += Brightness(edits.brightness.coerceIn(-1f, 1f))
        if (edits.contrast != 0f) effects += Contrast(edits.contrast.coerceIn(-1f, 1f))
        if (edits.saturation != 0f) effects += HslAdjustment.Builder().adjustSaturation(edits.saturation * 100f).build()
        effects += Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT)
        edits.overlay?.let { bmp ->
            val scaled = if (bmp.width != w || bmp.height != h) Bitmap.createScaledBitmap(bmp, w, h, true) else bmp
            effects += OverlayEffect(ImmutableList.of<androidx.media3.effect.TextureOverlay>(BitmapOverlay.createStaticBitmapOverlay(scaled)))
        }

        val edited = EditedMediaItem.Builder(item)
            .setRemoveAudio(edits.mute)
            .setEffects(Effects(emptyList(), effects))
            .build()

        val encoder = DefaultEncoderFactory.Builder(context.applicationContext)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(BITRATE)
                    .setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
                    .build()
            )
            .build()

        coroutineScope {
            var transformer: Transformer? = null
            val poll = launch {
                val holder = ProgressHolder()
                while (isActive) {
                    val t = transformer
                    if (t != null && t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(holder.progress / 100f)
                    }
                    delay(150)
                }
            }
            try {
                suspendCancellableCoroutine<Unit> { cont ->
                    val tr = Transformer.Builder(context.applicationContext)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .setEncoderFactory(encoder)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (cont.isActive) cont.resume(Unit)
                            }

                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                if (cont.isActive) cont.resumeWithException(exportException)
                            }
                        })
                        .build()
                    transformer = tr
                    cont.invokeOnCancellation {
                        // Transformer يُستعمل من خيطه (الرئيسي) فقط
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            tr.cancel()
                            out.delete()
                        }
                    }
                    tr.start(edited, out.absolutePath)
                }
            } catch (e: Exception) {
                out.delete()
                throw e
            } finally {
                poll.cancel()
            }
        }
        onProgress(1f)
        Output(out, (end - start) / 1000.0, w, h)
    }
}
