package com.chathala.hala.feature.chats.audio

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * مشغّل صوت بسيط — يشغّل عنصر واحد في المرة الواحدة.
 * يُشارَك كـ singleton في ChatViewModel حتى الضغط على رسالة أخرى يوقف السابقة.
 */
class AudioPlayer {

    private var player: MediaPlayer? = null
    private var currentId: String? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    data class PlaybackState(
        val playingId: String? = null,
        val positionMs: Int = 0,
        val durationMs: Int = 0
    )

    fun toggle(messageId: String, url: String) {
        if (_state.value.playingId == messageId) {
            stop()
        } else {
            play(messageId, url)
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        currentId = null
        _state.value = PlaybackState()
    }

    private fun play(messageId: String, url: String) {
        stop()
        try {
            val p = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.start()
                    _state.value = PlaybackState(
                        playingId = messageId,
                        positionMs = 0,
                        durationMs = mp.duration
                    )
                    startProgressTracker()
                }
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    stop(); true
                }
                prepareAsync()
            }
            player = p
            currentId = messageId
            _state.value = PlaybackState(playingId = messageId)
        } catch (e: Exception) {
            Log.e(TAG, "play failed: ${e.message}")
            stop()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val p = player ?: break
                val pos = runCatching { p.currentPosition }.getOrNull() ?: break
                _state.value = _state.value.copy(positionMs = pos)
                delay(200)
            }
        }
    }

    private companion object {
        const val TAG = "AudioPlayer"
    }
}
