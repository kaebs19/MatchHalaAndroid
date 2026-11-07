package com.chathala.hala.feature.editor

import androidx.compose.ui.layout.onSizeChanged
import com.chathala.hala.ui.components.dialogSafeBottom
import com.chathala.hala.ui.components.dialogSafeTop
import com.chathala.hala.ui.components.dialogSafeDrawing
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.view.TextureView
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FaceRetouchingOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.util.ImageCompressor
import com.chathala.hala.core.util.VideoCompressor
import com.chathala.hala.core.util.showToast
import com.chathala.hala.ui.components.FullScreenDialogWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** مكان استعمال المحرّر — يحدّد الأدوات والعنوان والزرّ. */
enum class EditorMode { CHAT, STORY }

/** صورة جاهزة للإرسال/النشر: JPEG ≤1MB مرسوم على الأصل. */
class EditedPhoto(
    val jpeg: ByteArray,
    val timerSec: Int?,
    /** ملصق المدينة في القصة — موضعه نسبي من الصورة المُرسلة */
    val locationTag: com.chathala.hala.feature.stories.data.StoryLocationTag? = null
)

private enum class Tool { CROP, TIMER, STICKERS, BLUR, COLORS, TRIM }

private val EMOJIS = listOf(
    "😀", "😂", "😍", "🥰", "😎", "🤩", "😜", "🤔", "😴", "🥳", "😇", "🙈",
    "❤️", "🔥", "✨", "🌹", "🌸", "⭐", "🎉", "🎂", "☕", "🍕", "⚽", "🎵",
    "👍", "👏", "🙏", "💪", "👑", "💎", "🌙", "☀️", "🌈", "🦋", "🐱", "🐶"
)

private val TIMER_OPTIONS = listOf(5, 10, 30)

/**
 * المحرّر الموحّد للصور (وضعا المحادثة والقصة). الوسيط بأكبر حجم، وشريط أيقونات
 * عمودي بلا نصوص يطفو في الجهة المقابلة (العربية: يسار). ضغطة تفتح لوحة قصيرة في
 * الأسفل تُسحب لإغلاقها، وضغطة ثانية على الأيقونة تغلقها، ولمس الوسيط يخفي الأدوات.
 * عدّة صور: شريط مصغّرات، لكل صورة تعديلاتها، ونقطة على المعدّلة.
 */
@Composable
fun PhotoEditorDialog(
    uris: List<Uri>,
    mode: EditorMode,
    onDismiss: () -> Unit,
    onDone: (List<EditedPhoto>) -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val edits = remember { mutableStateListOf(*Array(uris.size) { PhotoEdits() }) }
    var index by remember { mutableIntStateOf(0) }
    var tool by remember { mutableStateOf<Tool?>(null) }
    var chrome by remember { mutableStateOf(true) }
    var selectedSticker by remember { mutableStateOf<Long?>(null) }
    var busy by remember { mutableStateOf(false) }
    // ارتفاع الأدوات السفلية الفعلي — الوسيط ينتهي فوقها فلا يغطّيه التنبيه/الأزرار
    var bottomBarPx by remember { mutableIntStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // المعاينة: الأصل مصغّراً (≤1280) + الدوران/القصّ/الألوان/التمويه — بلا الملصقات (طبقة فوقها)
    val sources = remember { mutableStateListOf<Bitmap?>(*arrayOfNulls(uris.size)) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var faceCount by remember { mutableStateOf<Int?>(null) }
    val current = edits[index]
    LaunchedEffect(index) {
        if (sources[index] == null) sources[index] = ImageCompressor.decode(context, uris[index], 1280)
    }
    LaunchedEffect(index, sources[index], current.rotation, current.aspect, current.brightness, current.contrast, current.saturation, current.blurFaces) {
        val src = sources[index] ?: return@LaunchedEffect
        val base = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { PhotoRenderer.base(src, current) }
        preview = if (current.blurFaces) {
            val copy = base.copy(Bitmap.Config.ARGB_8888, true)
            faceCount = FaceBlur.apply(copy)
            copy
        } else base
    }

    fun update(block: (PhotoEdits) -> PhotoEdits) { edits[index] = block(edits[index]) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FullScreenDialogWindow()
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            // الوسيط
            Box(
                Modifier
                    .fillMaxSize()
                    .dialogSafeDrawing()
                    .padding(top = 56.dp, bottom = with(density) { bottomBarPx.toDp() }.coerceAtLeast(90.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = preview
                if (bmp == null) CircularProgressIndicator(color = Color.White)
                else Box(
                    Modifier
                        .aspectRatio(bmp.width.toFloat() / bmp.height)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                if (selectedSticker != null) selectedSticker = null
                                else { chrome = !chrome; tool = null }
                            })
                        }
                ) {
                    Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    StickerLayer(
                        stickers = current.stickers,
                        selectedId = selectedSticker,
                        onSelect = { selectedSticker = it },
                        onChange = { s -> update { e -> e.copy(stickers = e.stickers.map { if (it.id == s.id) s else it }) } },
                        onDelete = { id -> update { e -> e.copy(stickers = e.stickers.filterNot { it.id == id }) }; selectedSticker = null }
                    )
                }
            }

            EditorTopBar(
                title = S.get(if (mode == EditorMode.STORY) R.string.story_new else R.string.editor_title_photo),
                visible = chrome,
                onClose = onDismiss
            )

            // شريط الأدوات العمودي — الجهة المقابلة (End = يسار في العربية)
            val tools = buildList {
                add(Tool.CROP to Icons.Filled.Crop)
                if (mode == EditorMode.CHAT) add(Tool.TIMER to Icons.Filled.Timer)
                add(Tool.STICKERS to Icons.Filled.EmojiEmotions)
                add(Tool.BLUR to Icons.Filled.FaceRetouchingOff)
                add(Tool.COLORS to Icons.Filled.MoreHoriz)
            }
            ToolRail(tools, tool, chrome, Modifier.align(Alignment.CenterEnd)) { t -> tool = if (tool == t) null else t }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().dialogSafeBottom()) {
                ToolPanel(visible = tool != null && chrome, onDismiss = { tool = null }) {
                    when (tool) {
                        Tool.CROP -> CropPanel(current, { a -> update { it.copy(aspect = a) } }, { update { it.copy(rotation = (it.rotation + 90) % 360) } })
                        Tool.TIMER -> TimerPanel(current.timerSec) { v -> update { it.copy(timerSec = v) } }
                        Tool.STICKERS -> EditorStickerPanel(mode, EMOJIS) { st ->
                            update { it.copy(stickers = it.stickers + st) }
                            selectedSticker = st.id
                            tool = null
                        }
                        Tool.BLUR -> BlurPanel(current.blurFaces, faceCount) { on -> update { it.copy(blurFaces = on) } }
                        Tool.COLORS -> ColorsPanel(
                            current.brightness, current.contrast, current.saturation,
                            onChange = { b, c, s -> update { it.copy(brightness = b, contrast = c, saturation = s) } }
                        )
                        else -> Unit
                    }
                }
                Column(Modifier.fillMaxWidth().onSizeChanged { bottomBarPx = it.height }) {
                    if (uris.size > 1 && chrome) {
                        LazyRow(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            itemsIndexed(uris) { i, uri ->
                                Box {
                                    coil.compose.AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(BorderStroke(2.dp, if (i == index) Color.White else Color.Transparent), RoundedCornerShape(8.dp))
                                            .clickable { index = i; selectedSticker = null; faceCount = null }
                                    )
                                    if (edits[i].isEdited) Box(
                                        Modifier.align(Alignment.TopEnd).padding(3.dp).size(8.dp).clip(CircleShape).background(Color(0xFFFA7E1E))
                                    )
                                }
                            }
                        }
                    }
                    footer?.let { Box(Modifier.padding(horizontal = 16.dp)) { it() } }
                    Button(
                        onClick = {
                            busy = true
                            scope.launch {
                                val out = uris.indices.mapNotNull { i ->
                                    // التعديلات تُرسم على الأصل (≤2048) لا على المعاينة
                                    val original = ImageCompressor.decode(context, uris[i]) ?: return@mapNotNull null
                                    val rendered = PhotoRenderer.render(original, edits[i])
                                    val tag = edits[i].stickers.firstOrNull { it.locationPlace != null }?.let {
                                        com.chathala.hala.feature.stories.data.StoryLocationTag(
                                            place = it.locationPlace!!, x = it.x.toDouble(), y = it.y.toDouble(), size = it.size.toDouble()
                                        )
                                    }
                                    EditedPhoto(ImageCompressor.compress(rendered), edits[i].timerSec, tag)
                                }
                                busy = false
                                if (out.isEmpty()) context.showToast(S.get(R.string.profile_photo_read_failed)) else onDone(out)
                            }
                        },
                        enabled = !busy && preview != null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD62976), contentColor = Color.White)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else if (mode == EditorMode.STORY) Text(S.get(R.string.story_publish), fontWeight = FontWeight.Bold)
                        else {
                            Text(
                                if (uris.size > 1) S.get(R.string.editor_send_count, uris.size) else S.get(R.string.editor_send),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/** نتيجة محرّر الفيديو: الملف المضغوط والوضع (once/24h). */
class EditedVideo(val output: VideoCompressor.Output, val mode: String)

/**
 * محرّر الفيديو: قصّ (نافذة ≤15ث)، ملصقات، كتم، ألوان. **الضغط يجري أثناء المعاينة**
 * بنسبة تقدّم، ويُلغى ويُعاد عند تغيّر التعديلات. ثم «مرة واحدة» / «24 ساعة» + «إرسال».
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoEditorDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onDone: (EditedVideo) -> Unit
) {
    val context = LocalContext.current
    var meta by remember { mutableStateOf<VideoCompressor.Meta?>(null) }
    var edits by remember { mutableStateOf(VideoEdits()) }
    var tool by remember { mutableStateOf<Tool?>(null) }
    var chrome by remember { mutableStateOf(true) }
    var selectedSticker by remember { mutableStateOf<Long?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var output by remember { mutableStateOf<VideoCompressor.Output?>(null) }
    var failed by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(com.chathala.hala.feature.chats.data.VideoMode.ONCE) }
    val outDir = remember { File(context.cacheDir, "video_out") }

    LaunchedEffect(uri) {
        val m = VideoCompressor.probe(context, uri)
        if (m == null || m.width <= 0) {
            context.showToast(S.get(R.string.video_read_failed)); onDismiss(); return@LaunchedEffect
        }
        meta = m
        edits = edits.copy(endMs = minOf(m.durationMs, VideoCompressor.MAX_DURATION_MS))
    }

    // الضغط أثناء المعاينة — يُعاد عند تغيّر التعديلات (بعد سكون قصير)
    LaunchedEffect(meta, edits) {
        val m = meta ?: return@LaunchedEffect
        output?.file?.delete()
        output = null; failed = false; progress = 0f
        delay(700)
        val (w, h) = VideoCompressor.targetSize(m.width, m.height)
        val result = runCatching {
            VideoCompressor.compress(
                context, uri, m,
                VideoCompressor.Edits(
                    startMs = edits.startMs, endMs = edits.endMs, mute = edits.mute,
                    brightness = edits.brightness, contrast = edits.contrast, saturation = edits.saturation,
                    overlay = PhotoRenderer.stickerOverlay(w, h, edits.stickers)
                ),
                outDir
            ) { progress = it }
        }
        result.onSuccess { out ->
            if (out.file.length() > VideoCompressor.MAX_BYTES) {
                out.file.delete(); failed = true
                context.showToast(S.get(R.string.video_too_large_local))
            } else output = out
        }.onFailure { e -> if (e !is kotlinx.coroutines.CancellationException) failed = true else throw e }
    }

    // المشغّل: يعيد التشغيل داخل نافذة القصّ
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(edits.startMs, edits.endMs) { player.seekTo(edits.startMs) }
    LaunchedEffect(edits.mute) { player.volume = if (edits.mute) 0f else 1f }
    LaunchedEffect(Unit) {
        while (true) {
            if (player.currentPosition >= edits.endMs || player.playbackState == Player.STATE_ENDED) {
                player.seekTo(edits.startMs); player.play()
            }
            delay(100)
        }
    }

    fun close() {
        output?.file?.delete()
        onDismiss()
    }

    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FullScreenDialogWindow()
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val m = meta
            Box(
                Modifier
                    .fillMaxSize()
                    .dialogSafeDrawing()
                    .padding(top = 56.dp, bottom = 170.dp),
                contentAlignment = Alignment.Center
            ) {
                if (m == null) CircularProgressIndicator(color = Color.White)
                else Box(
                    Modifier
                        .aspectRatio(m.width.toFloat() / m.height)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                if (selectedSticker != null) selectedSticker = null
                                else { chrome = !chrome; tool = null }
                            })
                        }
                ) {
                    val filter = remember(edits.brightness, edits.contrast, edits.saturation) {
                        Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix(edits.brightness, edits.contrast, edits.saturation)) }
                    }
                    AndroidView(
                        factory = { ctx -> TextureView(ctx).also { player.setVideoTextureView(it) } },
                        update = { tv -> tv.setLayerType(View.LAYER_TYPE_HARDWARE, filter) },
                        modifier = Modifier.fillMaxSize()
                    )
                    StickerLayer(
                        stickers = edits.stickers,
                        selectedId = selectedSticker,
                        onSelect = { selectedSticker = it },
                        onChange = { s -> edits = edits.copy(stickers = edits.stickers.map { if (it.id == s.id) s else it }) },
                        onDelete = { id -> edits = edits.copy(stickers = edits.stickers.filterNot { it.id == id }); selectedSticker = null }
                    )
                }
            }

            EditorTopBar(S.get(R.string.editor_title_video), chrome, ::close)

            val tools = listOf(
                Tool.TRIM to Icons.Filled.ContentCut,
                Tool.STICKERS to Icons.Filled.EmojiEmotions,
                Tool.COLORS to Icons.Filled.MoreHoriz
            )
            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                ToolRail(tools, tool, chrome, Modifier) { t -> tool = if (tool == t) null else t }
                if (chrome) RailButton(
                    if (edits.mute) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    active = edits.mute
                ) { edits = edits.copy(mute = !edits.mute) }
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().dialogSafeBottom()) {
                ToolPanel(visible = tool != null && chrome, onDismiss = { tool = null }) {
                    when (tool) {
                        Tool.TRIM -> if (m != null) TrimPanel(m.durationMs, edits.startMs, edits.endMs) { s, e -> edits = edits.copy(startMs = s, endMs = e) }
                        Tool.STICKERS -> EditorStickerPanel(EditorMode.CHAT, EMOJIS) { st ->
                            edits = edits.copy(stickers = edits.stickers + st)
                            selectedSticker = st.id
                            tool = null
                        }
                        Tool.COLORS -> ColorsPanel(edits.brightness, edits.contrast, edits.saturation) { b, c, s ->
                            edits = edits.copy(brightness = b, contrast = c, saturation = s)
                        }
                        else -> Unit
                    }
                }
                // «مرة واحدة» / «24 ساعة»
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        com.chathala.hala.feature.chats.data.VideoMode.ONCE to S.get(R.string.video_mode_once),
                        com.chathala.hala.feature.chats.data.VideoMode.DAY to S.get(R.string.video_mode_24h)
                    ).forEach { (value, label) ->
                        val on = mode == value
                        Text(
                            label,
                            color = if (on) Color.Black else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) Color.White else Color.White.copy(alpha = 0.15f))
                                .clickable { mode = value }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
                Text(
                    S.get(if (mode == com.chathala.hala.feature.chats.data.VideoMode.ONCE) R.string.video_mode_once_hint else R.string.video_mode_24h_hint),
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), textAlign = TextAlign.Center
                )
                val ready = output
                Button(
                    onClick = { ready?.let { player.pause(); onDone(EditedVideo(it, mode)) } },
                    enabled = ready != null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD62976), contentColor = Color.White)
                ) {
                    when {
                        ready != null -> {
                            Text(S.get(R.string.editor_send), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        }
                        failed -> Text(S.get(R.string.video_compress_failed))
                        else -> Text(S.get(R.string.video_compressing, (progress * 100).toInt()))
                    }
                }
            }
        }
    }
}

// ── مكوّنات مشتركة ───────────────────────────────────────────────

@Composable
private fun EditorTopBar(title: String, visible: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Row(
            Modifier.fillMaxWidth().dialogSafeTop().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, S.get(R.string.action_close), tint = Color.White) }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ToolRail(tools: List<Pair<Tool, ImageVector>>, active: Tool?, visible: Boolean, modifier: Modifier, onTool: (Tool) -> Unit) {
    AnimatedVisibility(visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Column(
            Modifier.padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tools.forEach { (t, icon) -> RailButton(icon, active == t) { onTool(t) } }
        }
    }
}

@Composable
private fun RailButton(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 10.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.Black.copy(alpha = 0.45f))
            // حدّ فاتح: الزرّ الداكن كان يختفي على الخلفية السوداء خارج الصورة
            .border(1.dp, Color.White.copy(alpha = if (active) 0f else 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(22.dp)) }
}

/** لوحة قصيرة في الأسفل تُسحب للأسفل لإغلاقها. */
@Composable
private fun ToolPanel(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var drag by remember { mutableFloatStateOf(0f) }
    AnimatedVisibility(visible, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xE6202020))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { d -> drag = (drag + d).coerceAtLeast(0f) },
                    onDragStopped = { if (drag > 80f) onDismiss(); drag = 0f }
                )
                .padding(12.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CropPanel(edits: PhotoEdits, onAspect: (CropAspect) -> Unit, onRotate: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CropAspect.entries.size) { i ->
                val a = CropAspect.entries[i]
                FilterChip(
                    selected = edits.aspect == a,
                    onClick = { onAspect(a) },
                    label = { Text(if (a == CropAspect.FREE) S.get(R.string.editor_crop_free) else a.label) },
                    colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                )
            }
        }
        IconButton(onClick = onRotate) { Icon(Icons.Filled.RotateRight, S.get(R.string.editor_rotate), tint = Color.White) }
    }
}

@Composable
private fun TimerPanel(value: Int?, onChange: (Int?) -> Unit) {
    Column {
        Text(S.get(R.string.editor_timer_title), color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = value == null, onClick = { onChange(null) },
                label = { Text(S.get(R.string.editor_timer_off)) },
                colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
            )
            TIMER_OPTIONS.forEach { sec ->
                FilterChip(
                    selected = value == sec, onClick = { onChange(sec) },
                    label = { Text(S.get(R.string.editor_timer_seconds, sec)) },
                    colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                )
            }
        }
    }
}

@Composable
private fun EmojiPanel(onPick: (String) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(44.dp), Modifier.height(170.dp)) {
        items(EMOJIS) { e ->
            Text(e, fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.clickable { onPick(e) }.padding(6.dp))
        }
    }
}

@Composable
private fun BlurPanel(on: Boolean, faces: Int?, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(S.get(R.string.editor_blur_faces), color = Color.White, fontWeight = FontWeight.SemiBold)
            if (on && faces != null) Text(
                if (faces == 0) S.get(R.string.editor_no_faces) else S.plural(R.plurals.editor_faces_blurred, faces),
                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
            )
        }
        Switch(checked = on, onCheckedChange = onToggle)
    }
}

@Composable
private fun ColorsPanel(brightness: Float, contrast: Float, saturation: Float, onChange: (Float, Float, Float) -> Unit) {
    Column {
        LabeledSlider(S.get(R.string.editor_brightness), brightness) { onChange(it, contrast, saturation) }
        LabeledSlider(S.get(R.string.editor_contrast), contrast) { onChange(brightness, it, saturation) }
        LabeledSlider(S.get(R.string.editor_saturation), saturation) { onChange(brightness, contrast, it) }
        TextButton(onClick = { onChange(0f, 0f, 0f) }, modifier = Modifier.align(Alignment.End)) {
            Text(S.get(R.string.editor_reset), color = Color.White)
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Slider(value = value, onValueChange = onChange, valueRange = -0.5f..0.5f, modifier = Modifier.weight(1f))
    }
}

/** نافذة قصّ ≤15ث قابلة للسحب والتقصير. */
@Composable
private fun TrimPanel(durationMs: Long, startMs: Long, endMs: Long, onChange: (Long, Long) -> Unit) {
    val max = VideoCompressor.MAX_DURATION_MS
    Column {
        Text(
            S.get(R.string.editor_trim_label, (endMs - startMs) / 1000f),
            color = Color.White, fontSize = 13.sp
        )
        RangeSlider(
            value = startMs.toFloat()..endMs.toFloat(),
            onValueChange = { r ->
                var s = r.start.toLong()
                var e = r.endInclusive.toLong()
                // نافذة ≤15ث: سحب أحد الطرفين يجرّ الآخر
                if (e - s > max) { if (s != startMs) e = s + max else s = e - max }
                if (e - s < 1000) e = (s + 1000).coerceAtMost(durationMs)
                onChange(s.coerceAtLeast(0), e.coerceAtMost(durationMs))
            },
            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f)
        )
    }
}
