package com.chathala.hala.feature.stories.ui

import com.chathala.hala.ui.components.dialogSafeTop
import androidx.compose.foundation.layout.isImeVisible
import com.chathala.hala.ui.components.dialogSafeDrawing
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.ImageCompressor
import com.chathala.hala.core.util.showToast
import com.chathala.hala.feature.stories.data.STORY_TEXT_MAX
import com.chathala.hala.feature.stories.data.StoryErrorCodes
import com.chathala.hala.feature.stories.data.StoryLimitData
import com.chathala.hala.ui.components.FullScreenDialogWindow
import kotlinx.coroutines.launch
import java.io.File

/** يُفتح من أي مكان («+» على الصورة، بطاقة (+)، العارض) — [StoryComposerHost] يرسم الباقي. */
@Stable
class StoryComposerState {
    internal var quickSheet by mutableStateOf(false)
    internal var limitDialog by mutableStateOf<StoryLimitData?>(null)
    internal var photos by mutableStateOf<List<Uri>>(emptyList())
    internal var textComposer by mutableStateOf(false)
    internal var requestOpen by mutableIntStateOf(0)

    fun open() { requestOpen++ }
}

@Composable
fun rememberStoryComposerState(): StoryComposerState = remember { StoryComposerState() }

/**
 * المضيف الوحيد لإنشاء القصص: القائمة السريعة، المعرض/الكاميرا، شاشتا النشر،
 * وتنبيه الحدّ (الزرّ الذهبي). [onOpenPremium] يفتح صفحة الاشتراك ورقةً.
 */
@Composable
fun StoryComposerHost(
    state: StoryComposerState,
    onOpenPremium: () -> Unit,
    onOpenGuidelines: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HalaApp
    val manager = app.storyManager
    val stories by manager.myStories.collectAsState()
    val limits by manager.limits.collectAsState()
    val user by app.userRepository.currentUser.collectAsState(initial = null)
    val isPremium = user?.isPremium == true
    val remaining = (limits.max - stories.size).coerceAtLeast(0)

    LaunchedEffect(state.requestOpen) {
        if (state.requestOpen == 0) return@LaunchedEffect
        if (remaining > 0) state.quickSheet = true
        else if (!isPremium) state.limitDialog = StoryLimitData(max = limits.max, active = stories.size, freeMax = limits.freeMax, premiumMax = limits.premiumMax)
        else context.showToast(S.get(R.string.story_limit_premium_full, limits.max))
    }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = remaining.coerceAtLeast(2))
    ) { uris -> if (uris.isNotEmpty()) state.photos = uris.take(remaining.coerceAtLeast(1)) }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) state.photos = listOf(uri)
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { state.photos = listOf(it) }
    }

    val userId = user?.id
    val acked by (if (userId != null) app.appPreferences.storyPolicyAcknowledged(userId) else kotlinx.coroutines.flow.flowOf(true))
        .collectAsState(initial = true)

    if (state.quickSheet) {
        NewStorySheet(
            remaining = remaining,
            policyAcknowledged = acked,
            onGallery = {
                state.quickSheet = false
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (remaining > 1) multiPicker.launch(request) else singlePicker.launch(request)
            },
            onCamera = {
                state.quickSheet = false
                cameraUri = createStoryCameraUri(context).also { camera.launch(it) }
            },
            onText = { state.quickSheet = false; state.textComposer = true },
            onGuidelines = { state.quickSheet = false; onOpenGuidelines() },
            onDismiss = { state.quickSheet = false }
        )
    }

    fun onPublished() {
        if (userId != null) app.appScopeLaunch { app.appPreferences.acknowledgeStoryPolicy(userId) }
    }

    fun handleError(r: NetworkResult.Error) {
        if (r.code == StoryErrorCodes.LIMIT && !isPremium) {
            state.limitDialog = (r.payload as? StoryLimitData) ?: StoryLimitData()
        } else context.showToast(r.message)
    }

    if (state.photos.isNotEmpty()) {
        // المحرّر الموحّد بوضع «قصة»: بلا صورة مؤقتة، مع تنبيه المحتوى فوق «نشر»
        com.chathala.hala.feature.editor.PhotoEditorDialog(
            uris = state.photos,
            mode = com.chathala.hala.feature.editor.EditorMode.STORY,
            onDismiss = { state.photos = emptyList() },
            footer = {
                Column {
                    StoryContentWarning(expandedInitially = !acked, onGuidelines = onOpenGuidelines, dark = true)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        S.get(R.string.story_visible_24h_profile),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            },
            onDone = { photos ->
                state.photos = emptyList()
                // عدة صور تُرفع واحدة تلو الأخرى، وبطاقة رفع بحلقة تقدّم في الشريط
                app.appScopeLaunch {
                    var published = 0
                    for (photo in photos) {
                        when (val r = manager.publishImage(photo.jpeg, photo.locationTag, photo.caption)) {
                            is NetworkResult.Success -> published++
                            is NetworkResult.Error -> {
                                handleError(r)
                                break
                            }
                        }
                    }
                    if (published > 0) {
                        onPublished()
                        context.showToast(S.get(R.string.story_published))
                    }
                }
            }
        )
    }

    if (state.textComposer) {
        TextStoryComposer(
            policyAcknowledged = acked,
            onGuidelines = onOpenGuidelines,
            onDismiss = { state.textComposer = false },
            onPublish = { text, bg ->
                when (val r = manager.publishText(text, bg)) {
                    is NetworkResult.Success -> {
                        state.textComposer = false
                        onPublished()
                        context.showToast(S.get(R.string.story_published))
                        null
                    }
                    is NetworkResult.Error -> {
                        // نصّ مرفوض ← تبقى الشاشة مفتوحة لتعديله
                        if (r.code == StoryErrorCodes.TEXT_BLOCKED) r.message
                        else { state.textComposer = r.code == StoryErrorCodes.INVALID; handleError(r); null }
                    }
                }
            }
        )
    }

    state.limitDialog?.let { limit ->
        AlertDialog(
            onDismissRequest = { state.limitDialog = null },
            title = { Text(S.get(R.string.story_limit_title, limit.freeMax)) },
            text = { Text(S.get(R.string.story_limit_body, limit.premiumMax)) },
            confirmButton = {
                Button(onClick = { state.limitDialog = null; onOpenPremium() }) { Text(S.get(R.string.story_subscribe)) }
            },
            dismissButton = { TextButton(onClick = { state.limitDialog = null }) { Text(S.get(R.string.story_later)) } }
        )
    }
}

/** النشر يكمل بعد إغلاق الشاشات — نطاق على مستوى التطبيق على الخيط الرئيسي. */
private val publishScope = kotlinx.coroutines.MainScope()

@Suppress("UnusedReceiverParameter")
private fun HalaApp.appScopeLaunch(block: suspend () -> Unit) {
    publishScope.launch { block() }
}

private fun createStoryCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "story_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// ── القائمة السريعة ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewStorySheet(
    remaining: Int,
    policyAcknowledged: Boolean,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onText: () -> Unit,
    onGuidelines: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                S.get(R.string.story_new),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                SheetOption(
                    Icons.Filled.PhotoLibrary, Color(0xFF34C759), S.get(R.string.story_from_gallery),
                    if (remaining > 1) S.get(R.string.story_gallery_sub_many, remaining, remaining)
                    else S.get(R.string.story_gallery_sub_one),
                    onGallery
                )
                HorizontalDivider(Modifier.padding(start = 60.dp))
                SheetOption(Icons.Filled.CameraAlt, Color(0xFF007AFF), S.get(R.string.story_capture_now), S.get(R.string.story_capture_now_sub), onCamera)
                HorizontalDivider(Modifier.padding(start = 60.dp))
                SheetOption(Icons.Filled.TextFields, Color(0xFFD62976), S.get(R.string.story_text), S.get(R.string.story_text_sub), onText)
            }
            Spacer(Modifier.height(14.dp))
            StoryContentWarning(expandedInitially = !policyAcknowledged, onGuidelines = onGuidelines)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.get(R.string.story_disappear_24h), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SheetOption(icon: ImageVector, tint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(tint),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * تنبيه المحتوى: بطاقة وردية فاتحة ونصّ أحمر (لا صندوق أحمر صارخ). مفصّل حتى أول
 * قصة ينشرها المستخدم، ثم سطر واحد يتوسّع بالضغط.
 */
@Composable
fun StoryContentWarning(
    expandedInitially: Boolean,
    onGuidelines: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false
) {
    var expanded by remember(expandedInitially) { mutableStateOf(expandedInitially) }
    val red = Color(0xFFD32F2F)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            // الداكن معتم: الشفاف كان يذوب فوق الصور/الخلفيات البيضاء فلا يُقرأ
            .background(if (dark) Color(0xF2231A1D) else Color(0xFFFDECEE))
            .then(if (dark) Modifier.border(1.dp, Color(0x33FF8A80), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(enabled = !expanded) { expanded = true }
            .padding(12.dp)
            .animateContentSize()
    ) {
        val textColor = if (dark) Color(0xFFFF9E9E) else red
        if (!expanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WarningAmber, null, tint = textColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.get(R.string.story_warning_short), color = textColor, fontSize = 13.sp)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WarningAmber, null, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.get(R.string.story_warning_title), color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(S.get(R.string.story_warning_body), color = textColor, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                S.get(R.string.story_community_guidelines),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onGuidelines)
            )
        }
    }
}

// ── القصة النصّية ────────────────────────────────────────────────

/**
 * شاشة كاملة بالخلفية المختارة: حقل وسط الشاشة (خط عريض 26، أبيض، حتى 9 أسطر)،
 * عدّاد N/250 بوحدات UTF-16 مع القصّ بحروف كاملة (لا يقطع إيموجي)، 8 خلفيات.
 * [onPublish] يُرجع رسالة خطأ لإبقاء الشاشة مفتوحة (STORY_TEXT_BLOCKED) أو null.
 */
@Composable
private fun TextStoryComposer(
    policyAcknowledged: Boolean,
    onGuidelines: () -> Unit,
    onDismiss: () -> Unit,
    onPublish: suspend (String, Int) -> String?
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var background by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FullScreenDialogWindow()
        // النافذة لا تنزاح مع لوحة المفاتيح (كانت تدفع ✕ والعدّاد خارج الشاشة) —
        // safeDrawing يشمل لوحة المفاتيح فيتقلّص المحتوى بدلاً من ذلك
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect {
            @Suppress("DEPRECATION")
            dialogWindow?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
        Box(Modifier.fillMaxSize().background(storyBackgroundBrush(background)), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .dialogSafeDrawing()
                    .padding(horizontal = 12.dp)
            ) {
                // مساحة الشريط العلوي (مثبّت منفصلاً أدناه كي لا تدفعه لوحة المفاتيح خارج الشاشة)
                Spacer(Modifier.height(56.dp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.clampUtf16(STORY_TEXT_MAX); error = null },
                        textStyle = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 32.sp),
                        cursorBrush = SolidColor(Color.White),
                        maxLines = 9,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focus),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (text.isEmpty()) Text(
                                    S.get(R.string.story_text_placeholder),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                inner()
                            }
                        }
                    )
                }
                error?.let {
                    Text(
                        it,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCCD32F2F))
                            .padding(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    StoryBackgrounds.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(storyBackgroundBrush(i))
                                .border(if (i == background) 3.dp else 1.5.dp, Color.White, CircleShape)
                                .clickable { background = i }
                        )
                    }
                }
                // لوحة المفاتيح مفتوحة ← سطر واحد يتوسّع بالضغط (المساحة لا تتّسع للمفصّل)
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                val imeOpen = WindowInsets.isImeVisible
                StoryContentWarning(expandedInitially = !policyAcknowledged && !imeOpen, onGuidelines = onGuidelines, dark = true)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            error = onPublish(text.trim(), background)
                            busy = false
                        }
                    },
                    enabled = text.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(S.get(R.string.story_publish), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .dialogSafeTop()
                    .padding(horizontal = 12.dp)
            ) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, S.get(R.string.action_close), tint = Color.White) }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${text.length}/$STORY_TEXT_MAX",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
            }
        }
    }
}

/** يقصّ إلى [max] وحدة UTF-16 دون قطع حرف/إيموجي في المنتصف. */
internal fun String.clampUtf16(max: Int): String {
    if (length <= max) return this
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(this)
    var end = 0
    var next = it.next()
    while (next != java.text.BreakIterator.DONE && next <= max) {
        end = next
        next = it.next()
    }
    return substring(0, end)
}
