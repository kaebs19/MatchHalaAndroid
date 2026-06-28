package com.chathala.hala.feature.chats.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * تسجيل صوتي بسيط باستخدام MediaRecorder (AAC/m4a).
 *  - start(context) → يبدأ التسجيل في cacheDir ويُرجع Result<File>
 *  - stop() → يُنهي التسجيل ويُرجع File و duration (بالثواني)
 *  - cancel() → يُنهي ويحذف الملف
 */
class AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAtMs: Long = 0

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
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "start failed: ${e.message}", e)
            runCatching { rec.release() }
            file.delete()
            Result.failure(e)
        }
    }

    data class RecordingResult(val file: File, val durationSeconds: Int)

    fun stop(): Result<RecordingResult> {
        val rec = recorder ?: return Result.failure(IllegalStateException("no active recorder"))
        val file = output ?: return Result.failure(IllegalStateException("no output file"))
        return try {
            rec.stop()
            rec.release()
            val ms = System.currentTimeMillis() - startedAtMs
            val seconds = (ms / 1000).toInt().coerceAtLeast(1)
            recorder = null
            output = null
            Result.success(RecordingResult(file, seconds))
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
        runCatching { rec.stop() }
        runCatching { rec.release() }
        file?.delete()
        recorder = null
        output = null
    }

    private companion object {
        const val TAG = "AudioRecorder"
    }
}
