package com.chathala.hala.feature.stories.ui

import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.NoAdultContent
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.AddReaction
import com.chathala.hala.ui.components.dialogSafeBottom
import com.chathala.hala.ui.components.dialogSafeTop
import com.chathala.hala.ui.components.dialogSafeDrawing
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import com.chathala.hala.core.ads.AdGate
import com.chathala.hala.core.ads.NativeAdCard
import com.chathala.hala.core.ads.rememberNativeAd
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.showToast
import com.chathala.hala.feature.notifications.util.NotificationFormat
import com.chathala.hala.feature.reporting.data.ReportReason
import com.chathala.hala.feature.stories.data.Story
import com.chathala.hala.feature.stories.data.StoryErrorCodes
import com.chathala.hala.feature.stories.data.StoryManager
import com.chathala.hala.ui.components.FullScreenDialogWindow
import com.chathala.hala.ui.components.HalaAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** صاحب القصص المعروضة. */
data class StoryOwner(val id: String, val name: String, val image: String?)

/**
 * عارض القصص بملء الشاشة.
 *
 * - [isMine]: قصصي (من [StoryManager.myStories] حيّة) — يبدأ من [startStoryId] أو أحدث قصة.
 * - غير ذلك: يجلب قصص [owner] ويبدأ من **أول قصة لم تُشاهَد**.
 *
 * مؤقّت العدّ مربوط **بمعرّف** القصة الحالية لا بفهرسها: حذف قصة أثناء العرض
 * كان يجمّد عارض iOS لأن الفهرس بقي ثابتاً.
 */
@Composable
fun StoryViewer(
    owner: StoryOwner,
    isMine: Boolean,
    onDismiss: () -> Unit,
    startStoryId: String? = null,
    onMessage: (() -> Unit)? = null,
    onAddStory: (() -> Unit)? = null,
    onOpenPremium: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        FullScreenDialogWindow()
        StoryViewerContent(owner, isMine, onDismiss, startStoryId, onMessage, onAddStory, onOpenPremium)
    }
}

@Composable
private fun StoryViewerContent(
    owner: StoryOwner,
    isMine: Boolean,
    onDismiss: () -> Unit,
    startStoryId: String?,
    onMessage: (() -> Unit)?,
    onAddStory: (() -> Unit)?,
    onOpenPremium: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HalaApp
    val manager = app.storyManager
    val scope = rememberCoroutineScope()

    val myStories by manager.myStories.collectAsState()
    val muted by manager.muted.collectAsState()
    val currentUser by app.userRepository.currentUser.collectAsState(initial = null)
    val isPremium = currentUser?.isPremium == true

    // قصص الآخرين: تُجلب مرة. قصصي: حيّة من المدير.
    val othersStories = remember { mutableStateListOf<Story>() }
    var loading by remember { mutableStateOf(!isMine) }
    var currentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(owner.id, isMine) {
        if (isMine) {
            currentId = startStoryId?.takeIf { id -> myStories.any { it.id == id } }
                ?: myStories.lastOrNull()?.id
        } else {
            when (val r = manager.userStories(owner.id)) {
                is NetworkResult.Success -> {
                    othersStories.clear()
                    othersStories.addAll(r.data.stories.filterNot { it.isExpired() })
                    currentId = startStoryId?.takeIf { id -> othersStories.any { it.id == id } }
                        ?: othersStories.firstOrNull { it.viewed != true }?.id
                        ?: othersStories.firstOrNull()?.id
                }
                is NetworkResult.Error -> context.showToast(r.message)
            }
            loading = false
        }
    }

    val stories: List<Story> = if (isMine) myStories else othersStories
    val index = stories.indexOfFirst { it.id == currentId }
    val current = stories.getOrNull(index)

    // القصة الحالية حُذفت/انتهت ← التي حلّت محلّها، ولا قصص ← إغلاق
    var lastIndex by remember { mutableStateOf(0) }
    if (index >= 0) lastIndex = index
    LaunchedEffect(loading, stories.size, currentId) {
        if (!loading && current == null) {
            val replacement = stories.getOrNull(lastIndex) ?: stories.lastOrNull()
            if (replacement != null) currentId = replacement.id else onDismiss()
        }
    }

    // ── حالة الإيقاف ──
    var holding by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var viewersOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    // صفّ التفاعلات مفتوح (يوقف العدّ) + إيموجي يطفو عند التفاعل
    var reactionsOpen by remember { mutableStateOf(false) }
    var burst by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var imageReady by remember(currentId) { mutableStateOf(current?.isText == true) }

    // ── الإعلان بين القصص (لا لقصصي ولا لهلا+) ──
    val adsAllowed = !isMine && !isPremium && AdGate.rememberEnabled()
    val nativeAd = if (adsAllowed) rememberNativeAd(STORY_AD_SLOT) else null
    var adDue by remember { mutableStateOf(false) }
    var adThen by remember { mutableStateOf<(() -> Unit)?>(null) }

    val paused = holding || menuOpen || viewersOpen || confirmDelete || reportOpen || reactionsOpen || !imageReady || adThen != null

    fun proceed(action: () -> Unit) {
        if (adDue && nativeAd != null) {
            adDue = false
            manager.adShown()
            adThen = action
        } else action()
    }

    fun goNext() = proceed {
        val i = stories.indexOfFirst { it.id == currentId }
        val next = stories.getOrNull(i + 1)
        if (next != null) currentId = next.id else onDismiss()
    }

    fun close() = proceed(onDismiss)

    /** تفاعل متفائل: يُحدَّث محلياً فوراً ويُعاد عند الفشل. نفس الإيموجي مرة ثانية يزيله. */
    fun react(emoji: String) {
        val s = current ?: return
        val previous = s.myReaction
        val next = if (previous == emoji) null else emoji
        val i = othersStories.indexOfFirst { it.id == s.id }
        if (i >= 0) othersStories[i] = s.copy(myReaction = next, viewed = true)
        if (next != null) burst = next to System.nanoTime()
        reactionsOpen = false
        scope.launch {
            val r = manager.react(s.id, next)
            if (r is NetworkResult.Error) {
                val j = othersStories.indexOfFirst { it.id == s.id }
                if (j >= 0) othersStories[j] = othersStories[j].copy(myReaction = previous)
                context.showToast(r.message)
            }
        }
    }

    fun goPrev() {
        val i = stories.indexOfFirst { it.id == currentId }
        stories.getOrNull(i - 1)?.let { currentId = it.id }
    }

    // ── التقدّم: 5 ثوانٍ لكل قصة، يبدأ بعد تحميل الصورة ──
    val progress = remember(currentId) { Animatable(0f) }
    val goNextState by rememberUpdatedState(::goNext)
    LaunchedEffect(currentId, paused) {
        if (current == null || paused) return@LaunchedEffect
        val remaining = ((1f - progress.value) * StoryManager.STORY_DURATION_MS).toInt()
        progress.animateTo(1f, tween(remaining.coerceAtLeast(1), easing = LinearEasing))
        goNextState()
    }

    // ── تسجيل المشاهدة + عدّاد الإعلان ──
    val recorded = remember { HashSet<String>() }
    LaunchedEffect(currentId) {
        val s = current ?: return@LaunchedEffect
        if (isMine || !recorded.add(s.id)) return@LaunchedEffect
        manager.recordView(s.id)
        val i = othersStories.indexOfFirst { it.id == s.id }
        if (i >= 0) othersStories[i] = s.copy(viewed = true)
        manager.markViewed(owner.id, allViewed = othersStories.all { it.viewed == true || it.id == s.id })
        if (manager.countOthersStoryShown()) adDue = true
    }

    // ── لقطات الشاشة لقصص غيري (Android 14+) ──
    if (!isMine && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ScreenCaptureWatcher(currentId) { current?.id?.takeIf { it in recorded }?.let(manager::recordScreenshot) }
    }

    // ── عدّادات حيّة لقصصي (سوكت) — myStories حيّة أصلاً ──

    BackHandler { close() }

    // ── السحب للأسفل يغلق ──
    var dragY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dismissPx = with(density) { 140.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { dy -> dragY = (dragY + dy).coerceAtLeast(0f) },
                onDragStarted = { holding = true },
                onDragStopped = {
                    holding = false
                    if (dragY > dismissPx) close() else dragY = 0f
                }
            )
            .graphicsLayer {
                translationY = dragY
                val s = 1f - (dragY / (size.height.coerceAtLeast(1f))) * 0.25f
                scaleX = s; scaleY = s
            }
    ) {
        val adAction = adThen
        when {
            adAction != null && nativeAd != null -> StoryAdPage(nativeAd) {
                adThen = null
                adAction()
            }
            loading || current == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            else -> {
                // المحتوى
                if (current.isText) {
                    TextStoryCanvas(current.text.orEmpty(), current.background, Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = current.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        onSuccess = { imageReady = true },
                        onError = { imageReady = true },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!imageReady) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                }

                // منطقتا لمس: الثلث الأمامي (يمين في العربية) = السابقة، الباقي = التالية
                Row(Modifier.fillMaxSize()) {
                    TapZone(Modifier.weight(1f), onTap = ::goPrev, onHold = { holding = it })
                    TapZone(Modifier.weight(2f), onTap = ::goNext, onHold = { holding = it })
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .dialogSafeTop()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    SegmentedProgress(count = stories.size, index = index, progress = progress.value)
                    Spacer(Modifier.height(10.dp))
                    ViewerHeader(
                        name = if (isMine) S.get(R.string.story_yours) else owner.name,
                        image = owner.image,
                        time = NotificationFormat.timeAgo(current.createdAt),
                        onClose = ::close
                    )
                    if (isMine && current.isHeld) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            S.get(R.string.story_held_banner),
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xCCB26A00))
                                .padding(10.dp)
                        )
                    }
                }

                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .dialogSafeBottom()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isMine) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { viewersOpen = true }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${current.viewCount ?: 0}", color = Color.White, fontWeight = FontWeight.SemiBold)
                            val likes = current.reactionCount ?: 0
                            if (likes > 0) {
                                Spacer(Modifier.width(12.dp))
                                Text("❤️", fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("$likes", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            val shots = current.screenshotCount ?: 0
                            if (shots > 0) {
                                Spacer(Modifier.width(12.dp))
                                Icon(Icons.Filled.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("$shots", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (onAddStory != null && manager.remainingSlots > 0) {
                            RoundAction(Icons.Filled.Add) { onDismiss(); onAddStory() }
                        }
                        RoundAction(Icons.Filled.MoreHoriz) { menuOpen = true }
                    } else {
                        if (onMessage != null) {
                            Button(
                                onClick = { onDismiss(); onMessage() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ChatBubble, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(S.get(R.string.story_message_user, owner.name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else Spacer(Modifier.weight(1f))
                        // ❤️ سريع (ضغطة ثانية تزيله) + 😊 يفتح صفّ التفاعلات
                        val mine = current.myReaction
                        ReactionAction(
                            label = if (mine != null) mine else null,
                            onClick = { react(mine ?: "❤️") }
                        )
                        RoundAction(Icons.Filled.AddReaction) { reactionsOpen = !reactionsOpen }
                        RoundAction(Icons.Filled.MoreHoriz) { menuOpen = true }
                    }
                }
                if (!isMine && reactionsOpen) {
                    ReactionsRow(
                        selected = current.myReaction,
                        onPick = ::react,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .dialogSafeBottom()
                            .padding(bottom = 76.dp)
                    )
                }
                burst?.let { (emoji, key) -> ReactionBurst(emoji, key) { burst = null } }
            }
        }
    }

    // ── القوائم والحوارات ──
    val story = current
    if (menuOpen && story != null) {
        val isMuted = owner.id in muted
        StoryActionsSheet(
            actions = if (isMine) buildList {
                if (!story.isText) add(StoryAction(Icons.Filled.Download, S.get(R.string.story_save_to_photos)) {
                    scope.launch { StoryMediaSaver.save(context, story.imageUrl) }
                })
                add(StoryAction(Icons.Filled.Delete, S.get(R.string.story_delete), destructive = true) { confirmDelete = true })
            } else listOf(
                StoryAction(
                    if (isMuted) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    S.get(if (isMuted) R.string.story_unmute_user else R.string.story_mute_user, owner.name)
                ) {
                    scope.launch {
                        when (val r = manager.setMuted(owner.id, !isMuted)) {
                            is NetworkResult.Success -> context.showToast(
                                S.get(if (r.data) R.string.story_muted_done else R.string.story_unmuted_done, owner.name)
                            )
                            is NetworkResult.Error -> context.showToast(r.message)
                        }
                    }
                },
                StoryAction(Icons.Filled.Flag, S.get(R.string.story_report), destructive = true) { reportOpen = true }
            ),
            onDismiss = { menuOpen = false }
        )
    }
    if (confirmDelete && story != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(S.get(R.string.story_delete_confirm_title)) },
            text = { Text(S.get(R.string.story_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        when (val r = manager.delete(story.id)) {
                            is NetworkResult.Success -> context.showToast(S.get(R.string.story_deleted))
                            is NetworkResult.Error -> if (r.code != StoryErrorCodes.NOT_FOUND) context.showToast(r.message)
                        }
                    }
                }) { Text(S.get(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(S.get(R.string.action_cancel)) } }
        )
    }
    if (reportOpen && story != null) {
        StoryReportDialog(
            onDismiss = { reportOpen = false },
            onReason = { reason ->
                reportOpen = false
                scope.launch {
                    val r = app.reportRepository.reportUser(owner.id, reason, storyId = story.id)
                    context.showToast(
                        when (r) {
                            is NetworkResult.Success -> r.data
                            is NetworkResult.Error -> r.message
                        }
                    )
                }
                // ثم الانتقال للتالية فوراً
                goNext()
            }
        )
    }
    if (viewersOpen && story != null) {
        StoryViewersSheet(
            story = story,
            isPremium = isPremium,
            onDismiss = { viewersOpen = false },
            onOpenPremium = { viewersOpen = false; onDismiss(); onOpenPremium() }
        )
    }
}

@Composable
private fun TapZone(modifier: Modifier, onTap: () -> Unit, onHold: (Boolean) -> Unit) {
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)
    Box(
        modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // ضغط مطوّل يوقف العدّ حتى الرفع
                        val started = System.currentTimeMillis()
                        val longPress = kotlinx.coroutines.coroutineScope {
                            val job = launch { delay(220); hold(true) }
                            val released = tryAwaitRelease()
                            job.cancel()
                            released && System.currentTimeMillis() - started >= 220
                        }
                        if (longPress) hold(false)
                    },
                    onTap = { tap() }
                )
            }
    )
}

@Composable
private fun SegmentedProgress(count: Int, index: Int, progress: Float) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count.coerceAtLeast(1)) { i ->
            val fill = when {
                i < index -> 1f
                i == index -> progress
                else -> 0f
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun ViewerHeader(name: String, image: String?, time: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HalaAsyncImage(
            model = image,
            contentDescription = null,
            fallbackName = name,
            modifier = Modifier.size(36.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (time.isNotBlank()) Text(time, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = S.get(R.string.action_close), tint = Color.White)
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
    ) { Icon(icon, null, tint = Color.White) }
}

/** عنصر في قائمة ••• العارض. */
private class StoryAction(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val destructive: Boolean = false, val run: () -> Unit)

/** قائمة ••• ورقةً سفلية بأيقونات (بدل حوار نصّي). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StoryActionsSheet(actions: List<StoryAction>, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                actions.forEachIndexed { i, action ->
                    val tint = if (action.destructive) Color(0xFFE5484D) else MaterialTheme.colorScheme.onSurface
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss(); action.run() }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(action.icon, null, tint = tint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(action.label, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    if (i < actions.lastIndex) androidx.compose.material3.HorizontalDivider(
                        Modifier.padding(start = 52.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(S.get(R.string.action_cancel)) }
        }
    }
}

/**
 * ورقة البلاغ من داخل العارضين (قصة / صورة مؤقتة / فيديو): بطاقة لكل سبب بأيقونة
 * ووصف قصير، وملاحظة الخصوصية في الأسفل. اختيار السبب يرسل مباشرة.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StoryReportDialog(onDismiss: () -> Unit, onReason: (ReportReason) -> Unit, title: String = S.get(R.string.story_report)) {
    val reasons = listOf(
        Triple(ReportReason.INAPPROPRIATE, Icons.Filled.NoAdultContent, R.string.report_desc_inappropriate),
        Triple(ReportReason.HARASSMENT, Icons.Filled.FrontHand, R.string.report_desc_harassment),
        Triple(ReportReason.SPAM, Icons.Filled.Campaign, R.string.report_desc_spam)
    )
    val red = Color(0xFFE5484D)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Flag, null, tint = red, modifier = Modifier.size(28.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                S.get(R.string.report_sheet_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            reasons.forEach { (reason, icon, desc) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { onReason(reason) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(red.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, tint = red, modifier = Modifier.size(21.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(reason.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(S.get(desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.get(R.string.media_report_note), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDismiss) { Text(S.get(R.string.action_cancel)) }
        }
    }
}

/** بطاقة إعلان مدمج بملء العارض؛ «متابعة القصص» بعد مهلة قراءة قصيرة. */
@Composable
private fun StoryAdPage(ad: com.google.android.gms.ads.nativead.NativeAd, onContinue: () -> Unit) {
    var canContinue by remember { mutableStateOf(false) }
    LaunchedEffect(ad) {
        delay(com.chathala.hala.core.ads.AdConfig.STORY_AD_READ_DELAY_MS)
        canContinue = true
    }
    BackHandler(enabled = canContinue, onBack = onContinue)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .dialogSafeDrawing()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { NativeAdCard(ad) }
        OutlinedButton(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text(S.get(R.string.story_ad_continue)) }
    }
}

/**
 * كشف لقطات/تسجيل الشاشة: Android 14+ `registerScreenCaptureCallback`، و15+
 * `addScreenRecordingCallback` (تسجيل الشاشة يُعدّ لقطة لكل قصة تظهر أثناءه).
 * الأقدم بلا كشف. لا `FLAG_SECURE` هنا — القصص ليست مؤقتة كالصورة الخاصة.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun ScreenCaptureWatcher(storyKey: String?, onCapture: () -> Unit) {
    val activity = LocalContext.current.findActivityOrNull() ?: return
    val callback by rememberUpdatedState(onCapture)
    var recording by remember { mutableStateOf(false) }
    DisposableEffect(activity) {
        val executor = ContextCompat.getMainExecutor(activity)
        val shot = Activity.ScreenCaptureCallback { callback() }
        runCatching { activity.registerScreenCaptureCallback(executor, shot) }
        val stopRecording = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ScreenRecording.watch(activity, executor) { visible -> recording = visible }
        } else null
        onDispose {
            runCatching { activity.unregisterScreenCaptureCallback(shot) }
            stopRecording?.invoke()
        }
    }
    // أثناء التسجيل: كل قصة تظهر تُعدّ لقطة
    LaunchedEffect(storyKey, recording) { if (recording) callback() }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private object ScreenRecording {
    fun watch(activity: Activity, executor: java.util.concurrent.Executor, onChange: (Boolean) -> Unit): (() -> Unit)? {
        val wm = activity.windowManager
        val cb = java.util.function.Consumer<Int> { state ->
            onChange(state == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
        }
        return runCatching {
            val initial = wm.addScreenRecordingCallback(executor, cb)
            onChange(initial == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
            ({ runCatching { wm.removeScreenRecordingCallback(cb) }; Unit })
        }.getOrNull()
    }
}

private tailrec fun android.content.Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}

private const val STORY_AD_SLOT = "story_viewer_card"

/** زرّ القلب: فارغ بلا تفاعل، وبإيموجي تفاعلي إن وُجد. */
@Composable
private fun ReactionAction(label: String?, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) Text(label, fontSize = 22.sp)
        else Icon(Icons.Filled.FavoriteBorder, S.get(R.string.story_react), tint = Color.White)
    }
}

/** صفّ التفاعلات فوق الأزرار — المختار بخلفية فاتحة. */
@Composable
private fun ReactionsRow(selected: String?, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        com.chathala.hala.feature.stories.data.StoryReactions.forEach { e ->
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (e == selected) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                    .clickable { onPick(e) },
                contentAlignment = Alignment.Center
            ) { Text(e, fontSize = 26.sp) }
        }
    }
}

/** الإيموجي المختار يكبر ويطفو في وسط القصة ثم يختفي. */
@Composable
private fun ReactionBurst(emoji: String, key: Long, onDone: () -> Unit) {
    val scale = remember(key) { Animatable(0.3f) }
    val alpha = remember(key) { Animatable(1f) }
    LaunchedEffect(key) {
        scale.animateTo(1.6f, tween(320))
        delay(250)
        alpha.animateTo(0f, tween(300))
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            emoji,
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value; scaleY = scale.value
                this.alpha = alpha.value
            }
        )
    }
}
