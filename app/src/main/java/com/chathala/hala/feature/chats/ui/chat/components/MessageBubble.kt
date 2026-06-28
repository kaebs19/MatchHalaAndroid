package com.chathala.hala.feature.chats.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chathala.hala.feature.chats.data.Message
import com.chathala.hala.feature.notifications.util.NotificationFormat
import com.chathala.hala.ui.components.HalaAsyncImage

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    currentUserId: String? = null,
    isPending: Boolean = false,
    audioPlayingId: String? = null,
    audioPositionMs: Int = 0,
    audioDurationMs: Int = 0,
    disappearingExpiresAtMs: Long? = null,
    onToggleAudio: (Message) -> Unit = {},
    onLongPress: (Message) -> Unit = {},
    onViewDisappearing: (Message) -> Unit = {},
    onReplyTap: (String) -> Unit = {},
    revealedContent: String? = null,
    onRevealSensitive: (Message) -> Unit = {},
    onShowExternalPromoInfo: (Message) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // محتوى حساس — يُعرض للمُستقبِل فقط قبل الكشف
    if (message.hasFlaggedContent == true && !isMine && revealedContent == null) {
        SensitiveBubble(
            onReveal = { onRevealSensitive(message) },
            modifier = modifier
        )
        return
    }

    // deleted state
    if (message.isDeleted == true) {
        DeletedBubble(isMine = isMine, modifier = modifier)
        return
    }

    // system message — centered pill
    if (message.type == "system") {
        SystemMessageBubble(message = message, modifier = modifier)
        return
    }
    val bgColor = if (isMine)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    val shape = if (isMine)
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    else
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

    val pinkColor = Color(0xFFE91E8C)
    val isRevealed = revealedContent != null

    // عداد تنازلي للمحتوى المكشوف
    val remainingSeconds by produceState(initialValue = 30, key1 = revealedContent) {
        if (!isRevealed) return@produceState
        var secs = 30
        while (secs > 0) {
            delay(1_000L)
            secs--
            value = secs
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(shape)
                    .background(bgColor)
                    .then(
                        if (isRevealed) Modifier.border(
                            width = 1.5.dp,
                            color = pinkColor.copy(alpha = 0.5f),
                            shape = shape
                        ) else Modifier
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onLongPress(message) }
                    )
                    .padding(
                        horizontal = if (message.type == "image") 0.dp else 14.dp,
                        vertical = if (message.type == "image") 0.dp else 10.dp
                    ),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
            var showFullscreen by remember { mutableStateOf(false) }
            // Reply preview داخل الفقاعة — قابل للنقر للقفز
            val replyTo = message.replyTo
            if (replyTo != null) {
                Box(
                    modifier = Modifier.clickable {
                        replyTo.id?.let { onReplyTap(it) }
                    }
                ) {
                    ReplyPreviewInBubble(
                        reply = replyTo,
                        isMine = isMine,
                        textColor = textColor
                    )
                }
                Spacer(Modifier.size(6.dp))
            }

            when (message.type) {
                "image" -> {
                    if (message.disappearing?.enabled == true && !isMine) {
                        DisappearingImageContent(
                            message = message,
                            expiresAtMs = disappearingExpiresAtMs,
                            onView = { onViewDisappearing(message) }
                        )
                    } else {
                        ImageContent(
                            message = message,
                            isMine = isMine,
                            isPending = isPending,
                            onClick = { showFullscreen = true }
                        )
                    }
                    if (showFullscreen && !message.mediaUrl.isNullOrBlank()) {
                        FullscreenImageViewer(
                            url = message.mediaUrl,
                            onDismiss = { showFullscreen = false }
                        )
                    }
                }
                "audio" -> AudioContent(
                    message = message,
                    isMine = isMine,
                    isPlaying = audioPlayingId == message.id,
                    positionMs = audioPositionMs,
                    durationMs = audioDurationMs,
                    onToggle = { onToggleAudio(message) }
                )
                else -> {
                    val content = (revealedContent ?: message.content)?.takeIf { it.isNotBlank() }
                    if (content != null) {
                        LinkifiedText(
                            text = content,
                            color = textColor,
                            linkColor = if (isMine) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    // عداد تنازلي للمحتوى المكشوف
                    if (isRevealed && message.type != "image") {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "يُخفى بعد ${remainingSeconds}ث",
                            style = MaterialTheme.typography.labelSmall,
                            color = pinkColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            // الصور تعرض الوقت/الحالة كطبقة فوق الصورة نفسها
            if (message.type != "image") {
                Spacer(Modifier.size(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = NotificationFormat.timeAgoArabic(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (isMine) {
                        Spacer(Modifier.size(4.dp))
                        MineStatusIcon(
                            isPending = isPending,
                            isRead = message.isRead == true,
                            tint = textColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            } // end inner Column

            // شارة 🔞 للمحتوى المكشوف
            if (isRevealed && !isMine) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔞", style = MaterialTheme.typography.labelSmall)
                }
            }
        } // end Box

        // زر كشف الكلمات المحجوبة — للمرسل فقط عند الترويج الخارجي
        if (isMine && message.isExternalPromoBlocked == true) {
            Spacer(Modifier.size(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFF8C00).copy(alpha = 0.12f))
                    .clickable { onShowExternalPromoInfo(message) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "👁", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "كشف الكلمات المحجوبة",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF8C00)
                )
            }
        }

        // Reactions pills تحت الفقاعة
        message.reactions?.takeIf { it.isNotEmpty() }?.let { list ->
            Spacer(Modifier.size(4.dp))
            ReactionPills(
                reactions = list,
                currentUserId = currentUserId,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun ReplyPreviewInBubble(
    reply: com.chathala.hala.feature.chats.data.MessageReply,
    isMine: Boolean,
    textColor: androidx.compose.ui.graphics.Color
) {
    val accent = if (isMine) textColor else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(textColor.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .background(accent)
        )
        Spacer(Modifier.size(6.dp))
        Column {
            Text(
                text = reply.sender?.name ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.9f)
            )
            Text(
                text = when (reply.type) {
                    "image" -> "📷 صورة"
                    "audio" -> "🎙️ صوت"
                    else -> reply.content?.takeIf { it.isNotBlank() } ?: "…"
                },
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ImageContent(
    message: Message,
    isMine: Boolean,
    isPending: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(300.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        HalaAsyncImage(
            model = message.mediaUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // شارة مصدر الصورة (كاميرا / معرض) أعلى البداية
        message.imageSource?.let { src ->
            val (icon, label) = when (src.lowercase()) {
                "camera" -> Icons.Filled.PhotoCamera to "كاميرا"
                else -> Icons.Filled.PhotoLibrary to "معرض"
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // شارة الاختفاء (للمُرسِل) أعلى النهاية
        if (isMine && message.disappearing?.enabled == true) {
            val dur = message.disappearing.duration ?: 0
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "${dur}ث",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // طبقة الوقت + الحالة أسفل الصورة مع تدرّج داكن
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.align(if (isMine) Alignment.BottomStart else Alignment.BottomEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = NotificationFormat.timeAgoArabic(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                if (isMine) {
                    Spacer(Modifier.size(4.dp))
                    MineStatusIcon(
                        isPending = isPending,
                        isRead = message.isRead == true,
                        tint = Color.White
                    )
                }
            }
        }
    }

    val caption = message.content?.takeIf { it.isNotBlank() }
    if (caption != null) {
        Spacer(Modifier.size(6.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** عارض صورة بملء الشاشة مع تكبير/تصغير بإصبعين. */
@Composable
private fun FullscreenImageViewer(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 4f)
            offsetX += panChange.x
            offsetY += panChange.y
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        ) {
            HalaAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .transformable(transformState)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "إغلاق",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun DisappearingImageContent(
    message: Message,
    expiresAtMs: Long?,
    onView: () -> Unit
) {
    val durationSec = message.disappearing?.duration ?: 10
    val nowMs by produceNow()
    val isViewing = expiresAtMs != null && nowMs < expiresAtMs
    val isExpired = expiresAtMs != null && nowMs >= expiresAtMs
    val remainingSec = if (isViewing && expiresAtMs != null)
        ((expiresAtMs - nowMs) / 1000).toInt().coerceAtLeast(0)
    else durationSec

    val phase = when {
        isExpired -> "expired"
        isViewing -> "viewing"
        else -> "locked"
    }
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // تلاشٍ سلس عند انتهاء الصلاحية — تأثير «التدمير الذاتي»
        androidx.compose.animation.Crossfade(
            targetState = phase,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
            label = "disappearing-phase"
        ) { p ->
        when (p) {
            "expired" -> {
                // انتهت
                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "انتهت صلاحية الصورة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            "viewing" -> {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    HalaAsyncImage(
                        model = message.mediaUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(240.dp).height(280.dp)
                    )
                    // عداد تنازلي في الزاوية
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⏱  $remainingSec",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            else -> {
                // غير مشاهدة بعد — blurred placeholder
                Column(
                    modifier = Modifier
                        .fillMaxWidth().fillMaxHeight()
                        .clickable { onView() },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👁️",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "اضغط للمشاهدة",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "صورة تختفي خلال ${durationSec}ث",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
    }
}

/** كل ثانية، يُرجع الوقت الحالي لتحديث الـ countdown. */
@Composable
private fun produceNow(): androidx.compose.runtime.State<Long> {
    return androidx.compose.runtime.produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(250)
            value = System.currentTimeMillis()
        }
    }
}

@Composable
private fun SystemMessageBubble(message: Message, modifier: Modifier = Modifier) {
    val label = remember(message.content) {
        val c = message.content?.trim().orEmpty()
        if (c.startsWith("{")) {
            runCatching {
                val obj = org.json.JSONObject(c)
                obj.optString("textAr").takeIf { it.isNotBlank() }
                    ?: obj.optString("textEn").takeIf { it.isNotBlank() }
                    ?: c
            }.getOrDefault(c)
        } else c
    }
    if (label.isBlank()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DeletedBubble(isMine: Boolean, modifier: Modifier = Modifier) {
    val shape = if (isMine)
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    else
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = shape
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "🚫  تم حذف الرسالة",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AudioContent(
    message: Message,
    isMine: Boolean,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onToggle: () -> Unit
) {
    val accentColor = if (isMine)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.primary

    val totalSeconds = (message.audioDuration ?: (durationMs / 1000)).coerceAtLeast(1)
    val currentSeconds = (positionMs / 1000).coerceAtMost(totalSeconds)
    val progress = if (isPlaying && durationMs > 0)
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(min = 180.dp, max = 260.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { progress },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.size(4.dp))
            val displaySeconds = if (isPlaying) currentSeconds else totalSeconds
            Text(
                text = ChatInputBarFormatter.formatDuration(displaySeconds),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SensitiveBubble(
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .clip(shape)
                .background(Color(0xFF2D0A1F))
                .border(1.dp, Color(0xFFE91E8C).copy(alpha = 0.4f), shape)
                .clickable { onReveal() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "محتوى حساس",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = Color(0xFFE91E8C)
                )
                Spacer(Modifier.size(6.dp))
                Text(text = "🔞", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = "اضغط للكشف عن المحتوى",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE91E8C).copy(alpha = 0.7f)
            )
        }
    }
}

// wrapper لتفادي import circular — ChatInputBar.formatDuration private
internal object ChatInputBarFormatter {
    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}

@Composable
private fun MineStatusIcon(
    isPending: Boolean,
    isRead: Boolean,
    tint: androidx.compose.ui.graphics.Color
) {
    Icon(
        imageVector = when {
            isPending -> Icons.Filled.Schedule
            isRead -> Icons.Filled.DoneAll
            else -> Icons.Filled.Done
        },
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(14.dp)
    )
}
