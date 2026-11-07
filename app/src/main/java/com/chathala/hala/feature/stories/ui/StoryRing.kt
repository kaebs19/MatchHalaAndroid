package com.chathala.hala.feature.stories.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.chathala.hala.HalaApp
import com.chathala.hala.feature.stories.data.StoryStatus
import androidx.compose.ui.platform.LocalContext

/** ألوان حلقة القصة (مثل iOS). */
val StoryRingColors = listOf(
    Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5), Color(0xFFFEDA75)
)

val StoryRingBrush: Brush = Brush.linearGradient(StoryRingColors.dropLast(1))

enum class StoryRingState { None, Unseen, Seen }

fun StoryStatus?.ringState(): StoryRingState = when {
    this == null || count <= 0 -> StoryRingState.None
    hasUnseen -> StoryRingState.Unseen
    else -> StoryRingState.Seen
}

/**
 * حالة حلقة مستخدم من [com.chathala.hala.feature.stories.data.StoryManager]
 * (المكتوم بلا حلقة).
 */
@Composable
fun rememberStoryRing(userId: String?): StoryRingState {
    if (userId.isNullOrBlank()) return StoryRingState.None
    val manager = (LocalContext.current.applicationContext as HalaApp).storyManager
    val statuses by manager.statuses.collectAsState()
    val muted by manager.muted.collectAsState()
    if (userId in muted) return StoryRingState.None
    return statuses[userId].ringState()
}

/**
 * يلفّ صورة دائرية بحلقة القصة: تدرّج دائري يدور ببطء (دورة/6ث) ما دامت فيه قصة غير
 * مشاهدة، وبعد المشاهدة حلقة رمادية رفيعة ثابتة. بين الحلقة والصورة فاصل بلون
 * الخلفية. بلا قصص يُرسم المحتوى كما هو.
 */
@Composable
fun StoryRingBox(
    state: StoryRingState,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 3.5.dp,
    gap: Dp = 2.5.dp,
    gapColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    if (state == StoryRingState.None) {
        Box(modifier, contentAlignment = Alignment.Center, content = content)
        return
    }
    val unseen = state == StoryRingState.Unseen
    val angle: State<Float> = if (unseen) {
        rememberInfiniteTransition(label = "storyRing").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart),
            label = "storyRingAngle"
        )
    } else remember { mutableFloatStateOf(0f) }
    val seenColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .then(if (unseen) Modifier.shadow(6.dp, CircleShape, ambientColor = Color(0xFFD62976), spotColor = Color(0xFFD62976)) else Modifier)
                .rotate(angle.value)
        ) {
            val w = if (unseen) ringWidth.toPx() else 1.5.dp.toPx()
            val r = this.size.minDimension / 2f - w / 2f
            if (unseen) {
                drawCircle(Brush.sweepGradient(StoryRingColors), radius = r, style = Stroke(w))
            } else {
                drawCircle(seenColor, radius = r, style = Stroke(w))
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(ringWidth)
                .clip(CircleShape)
                .background(gapColor)
                .padding(gap)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * زرّ «قصة جديدة (2)» / «القصص (2)» في رأس بروفايل الآخرين وصورته الكاملة.
 * غير المشاهدة بتدرّج الحلقة، والمشاهدة بخلفية داكنة شفافة.
 */
@Composable
fun StoriesPill(status: StoryStatus?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val count = status?.count ?: 0
    if (count <= 0) return
    val unseen = status?.hasUnseen == true
    androidx.compose.foundation.layout.Row(
        modifier
            .clip(CircleShape)
            .then(if (unseen) Modifier.background(StoryRingBrush) else Modifier.background(Color.Black.copy(alpha = 0.45f)))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Filled.AutoStories,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        androidx.compose.material3.Text(
            com.chathala.hala.core.i18n.S.get(
                if (unseen) com.chathala.hala.R.string.story_pill_new else com.chathala.hala.R.string.story_pill_seen,
                count
            ),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
    }
}

/**
 * شارة القصة على بطاقة الاكتشاف: صورة صغيرة بحلقة القصص (تدور ما دامت غير مشاهدة)
 * + «قصة جديدة» أو «قصة». الضغط يفتح العارض.
 */
@Composable
fun StoryBadge(
    userId: String?,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    name: String? = null,
    onClick: (() -> Unit)? = null,
    /** البروفايل يعرض القصص حتى للمكتوم (الكتم يخفي الحلقة من القوائم فقط). */
    ignoreMute: Boolean = false,
    /** للبطاقات الصغيرة (شبكة البحث): أصغر وبلا صورة. */
    compact: Boolean = false
) {
    val ring = if (ignoreMute) {
        val statuses by (LocalContext.current.applicationContext as HalaApp).storyManager.statuses.collectAsState()
        statuses[userId].ringState()
    } else rememberStoryRing(userId)
    if (ring == StoryRingState.None) return
    val unseen = ring == StoryRingState.Unseen
    if (compact) {
        androidx.compose.material3.Text(
            com.chathala.hala.core.i18n.S.get(if (unseen) com.chathala.hala.R.string.story_badge_new else com.chathala.hala.R.string.story_badge_seen),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = modifier
                .clip(CircleShape)
                .then(if (unseen) Modifier.background(StoryRingBrush) else Modifier.background(Color.Black.copy(alpha = 0.5f)))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 9.dp, vertical = 4.dp)
        )
        return
    }
    androidx.compose.foundation.layout.Row(
        modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StoryRingBox(state = ring, modifier = Modifier.size(34.dp), ringWidth = 2.5.dp, gap = 1.5.dp, gapColor = Color.Black) {
            com.chathala.hala.ui.components.HalaAsyncImage(
                model = avatarUrl,
                contentDescription = null,
                fallbackName = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        androidx.compose.material3.Text(
            com.chathala.hala.core.i18n.S.get(if (unseen) com.chathala.hala.R.string.story_badge_new else com.chathala.hala.R.string.story_badge_seen),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

/**
 * زرّ «إضافة قصة» صغير (رأس الاكتشاف بجوار «الزوار»): صورتي بحلقة إن كانت لي قصص،
 * وشارة + وردية.
 */
@Composable
fun AddStoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as HalaApp
    val user by app.userRepository.currentUser.collectAsState(initial = null)
    val mine by app.storyManager.myStories.collectAsState()
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        StoryRingBox(
            state = if (mine.isEmpty()) StoryRingState.None else StoryRingState.Unseen,
            modifier = Modifier.fillMaxSize(),
            ringWidth = 2.dp,
            gap = 1.5.dp
        ) {
            com.chathala.hala.ui.components.HalaAsyncImage(
                model = user?.profileImage,
                contentDescription = com.chathala.hala.core.i18n.S.get(com.chathala.hala.R.string.story_new),
                fallbackName = user?.name,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(1.5.dp)
                .clip(CircleShape)
                .background(Color(0xFFD62976)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

/**
 * زرّ «القصص» في شريط الاكتشاف: بارتفاع الشريط كاملاً، وحلقة فيها نقطة «جديد» في قلبها
 * تنبض بهدوء ما دامت هناك قصص لم تُشاهد — وتثبت مع «تقليل الحركة» (مقياس الحركة = 0).
 */
@Composable
fun StoriesFeedButton(loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val app = LocalContext.current.applicationContext as HalaApp
    val statuses by app.storyManager.statuses.collectAsState()
    val hasUnseen = statuses.values.any { it.count > 0 && it.hasUnseen }
    androidx.compose.foundation.layout.Row(
        modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = if (compact) 9.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (loading) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                StoryRingBox(
                    state = if (hasUnseen) StoryRingState.Unseen else StoryRingState.Seen,
                    modifier = Modifier.fillMaxSize(),
                    ringWidth = 2.5.dp,
                    gap = 1.5.dp
                ) {
                    if (hasUnseen) StoriesPulseDot(Modifier.align(Alignment.Center))
                }
            }
        }
        // الشاشات الضيّقة: الحلقة وحدها كي لا يُعصر عنوان الاكتشاف
        if (!compact) {
            androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
            androidx.compose.material3.Text(
                com.chathala.hala.core.i18n.S.get(com.chathala.hala.R.string.stories_feed_button),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StoriesPulseDot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduceMotion = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
    val pulse = if (reduceMotion) null else androidx.compose.animation.core.rememberInfiniteTransition(label = "storiesPulse")
    val scale = pulse?.animateFloat(
        initialValue = 0.8f, targetValue = 1.25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "storiesPulseScale"
    )?.value ?: 1f
    Box(
        modifier
            .size(8.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (reduceMotion) 1f else 0.75f + (scale - 0.8f) / 0.45f * 0.25f }
            .clip(CircleShape)
            .background(StoryRingBrush)
    )
}
