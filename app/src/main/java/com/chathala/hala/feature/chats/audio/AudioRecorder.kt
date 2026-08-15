package com.chathala.hala.feature.chats.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10

/**
 * تسجيل صوتي بسيط باستخدام MediaRecorder (AAC/m4a).
 *  - start(context) → يبدأ التسجيل في cacheDir ويُرجع Result<File>
 *  - stop() → يُنهي التسجيل ويُرجع File و duration (بالثواني) و waveform
 *  - cancel() → يُنهي ويحذف الملف
 *
 * الـ waveform يُلتقط كل 100ms من maxAmplitude ويُطبَّع إلى 0..1 بمقياس ديسيبل
 * مطابق لما يرسله iOS ((dB + 50) / 50) حتى تتشابه الموجة على المنصّتين.
 */
class AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAtMs: Long = 0

    private val scope = CoroutineScope(Dispatchers.Default)
    private var amplitudeJob: Job? = null
    private val levels = mutableListOf<Double>()

    val isRecording: Boolean get() = recorder != null

    fun start(context: Context): Result<File> {
        if (isRecording) return Result.failure(IllegalStateException("recorder busy"))
        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        return try {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(22_050)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            output = file
            startedAtMs = System.currentTimeMillis()
            startAmplitudeTracker()
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "start failed: ${e.message}", e)
            runCatching { rec.release() }
            file.delete()
            Result.failure(e)
        }
    }

    data class RecordingResult(
        val file: File,
        val durationSeconds: Int,
        val waveform: List<Double> = emptyList()
    )

    fun stop(): Result<RecordingResult> {
        val rec = recorder ?: return Result.failure(IllegalStateException("no active recorder"))
        val file = output ?: return Result.failure(IllegalStateException("no output file"))
        val waveform = stopAmplitudeTracker()
        return try {
            rec.stop()
            rec.release()
            val ms = System.currentTimeMillis() - startedAtMs
            val seconds = (ms / 1000).toInt().coerceAtLeast(1)
            recorder = null
            output = null
            Result.success(RecordingResult(file, seconds, waveform))
        } catch (e: Exception) {
            Log.e(TAG, "stop failed: ${e.message}", e)
            runCatching { rec.release() }
            file.delete()
            recorder = null
            output = null
            Result.failure(e)
        }
    }

    fun cancel() {
        val rec = recorder ?: return
        val file = output
        stopAmplitudeTracker()
        runCatching { rec.stop() }
        runCatching { rec.release() }
        file?.delete()
        recorder = null
        output = null
    }

    // ── waveform ──────────────────────────────────────────────────

    private fun startAmplitudeTracker() {
        amplitudeJob?.cancel()
        synchronized(levels) { levels.clear() }
        amplitudeJob = scope.launch {
            while (true) {
                delay(SAMPLE_INTERVAL_MS)
                val amp = runCatching { recorder?.maxAmplitude }.getOrNull() ?: break
                val level = normalize(amp)
                synchronized(levels) {
                    if (levels.size < MAX_SAMPLES) levels.add(level)
                }
            }
        }
    }

    private fun stopAmplitudeTracker(): List<Double> {
        amplitudeJob?.cancel()
        amplitudeJob = null
        return synchronized(levels) { levels.toList() }
    }

    /** يحوّل السعة (0..32767) إلى مستوى 0..1 بمقياس ديسيبل مطابق لـ iOS. */
    private fun normalize(amplitude: Int): Double {
        if (amplitude <= 0) return 0.0
        val db = 20.0 * log10(amplitude.toDouble() / MAX_AMPLITUDE)
        return ((db + 50.0) / 50.0).coerceIn(0.0, 1.0)
    }

    private companion object {
        const val TAG = "AudioRecorder"
        const val SAMPLE_INTERVAL_MS = 100L
        const val MAX_AMPLITUDE = 32767.0
        const val MAX_SAMPLES = 300      // 30 ثانية × 10 عيّنات/ثانية
    }
}
