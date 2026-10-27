package com.chathala.hala.feature.chats.ui.chat.components

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.feature.chats.data.Message
import com.chathala.hala.feature.chats.data.VideoMode
import com.chathala.hala.feature.reporting.data.ReportReason
import com.chathala.hala.feature.stories.ui.StoryReportDialog
import com.chathala.hala.ui.components.FullScreenDialogWindow
import kotlinx.coroutines.delay
import java.io.File

/** حالة صفّ الفيديو. */
enum class VideoRowState { SENDING, FAILED, NOT_OPENED, VIEWED, EXPIRED, LOADING }

/**
 * صفّ فيديو مدمج **بلا معاينة**: 🎥 + المدة + الحالة (لم يُفتح/شوهد/انتهى/يُرسل/فشل).
 * المستلم يضغط للتنزيل ثم المشاهدة.
 */
@Composable
fun VideoMessageContent(
    message: Message,
    isMine: Boolean,
    state: VideoRowState,
    textColor: Color,
    onOpen: () -> Unit
) {
    val seconds = (message.videoDuration ?: message.disappearing?.duration?.toDouble() ?: 0.0).let { Math.round(it).toInt() }
    val once = message.disappearing?.mode != VideoMode.DAY
    val openable = state == VideoRowState.NOT_OPENED || (state == VideoRowState.VIEWED && (!once || isMine))
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = openable, onClick = onOpen)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(textColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (state == VideoRowState.LOADING || state == VideoRowState.SENDING) {
                CircularProgressIndicator(Modifier.size(20.dp), color = textColor, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Videocam, null, tint = textColor.copy(alpha = if (state == VideoRowState.EXPIRED) 0.5f else 1f))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                S.get(R.string.video_row_title, seconds),
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                when (state) {
                    VideoRowState.SENDING -> S.get(R.string.video_state_sending)
                    VideoRowState.FAILED -> S.get(R.string.video_state_failed)
                    VideoRowState.LOADING -> S.get(R.string.video_state_loading)
                    VideoRowState.EXPIRED -> S.get(R.string.video_state_expired)
                    VideoRowState.VIEWED -> S.get(if (isMine) R.string.video_state_viewed else if (once) R.string.video_state_expired else R.string.video_state_replay)
                    VideoRowState.NOT_OPENED -> S.get(if (isMine) R.string.video_state_not_opened else R.string.video_state_tap)
                } + " · " + S.get(if (once) R.string.video_mode_once else R.string.video_mode_24h),
                color = textColor.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * عارض الفيديو المؤقت بملء الشاشة **بـ FLAG_SECURE** (مقابل ScreenCaptureProtected في iOS).
 * يُحذف الملف المحلي عند الإغلاق، ويتوقف الفيديو عند فتح قائمة البلاغ. ExoPlayer يطلب
 * تركيز الصوت ويتخلّى عنه عند الإغلاق فيعود الصوت لوضعه السابق.
 * [deadlineMs]: لفيديو 24h يُغلق العارض إن انقضى يومه أثناء المشاهدة.
 */
@OptIn(UnstableApi::class)
@Composable
fun DisappearingVideoViewer(
    file: File,
    deadlineMs: Long?,
    canReport: Boolean,
    onReport: (ReportReason) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var reportOpen by remember { mutableStateOf(false) }
    var position by remember { mutableFloatStateOf(0f) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                /* handleAudioFocus = */ true
            )
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            player.release()
            file.delete()
        }
    }
    LaunchedEffect(reportOpen) { if (reportOpen) player.pause() else player.play() }
    LaunchedEffect(Unit) {
        while (true) {
            val d = player.duration.takeIf { it > 0 } ?: 1L
            position = (player.currentPosition.toFloat() / d).coerceIn(0f, 1f)
            if (deadlineMs != null && System.currentTimeMillis() >= deadlineMs) { onClose(); break }
            if (player.playbackState == Player.STATE_ENDED) { onClose(); break }
            delay(100)
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FullScreenDialogWindow()
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        BackHandler(onBack = onClose)
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Column(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { position },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, S.get(R.string.action_close), tint = Color.White) }
                    if (canReport) IconButton(onClick = { reportOpen = true }) {
                        Icon(Icons.Filled.Flag, S.get(R.string.media_report), tint = Color.White)
                    }
                }
            }
        }
    }
    if (reportOpen) {
        StoryReportDialog(
            title = S.get(R.string.media_report),
            onDismiss = { reportOpen = false },
            onReason = { reason -> reportOpen = false; onReport(reason) }
        )
    }
}
