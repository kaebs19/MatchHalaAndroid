package com.chathala.hala.feature.discover.ui.components

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chathala.hala.core.util.HapticHelper
import com.chathala.hala.core.util.ProfileFormatter
import com.chathala.hala.feature.discover.data.DiscoverCard
import kotlin.math.roundToInt
import com.chathala.hala.ui.components.HalaAsyncImage

/**
 * اتجاه السحب — يُرجَع من onSwiped.
 */
enum class SwipeDirection { LEFT, RIGHT, UP }

/**
 * بطاقة مستخدم بملء الشاشة — مطابقة iOS SwipeCardView.
 * - سحب لليمين = إعجاب، يسار = تخطي، أعلى = Super Like
 * - دوران خفيف + أختام LIKE/NOPE/SUPER
 * - نقر على نصف الشاشة الأيمن/الأيسر = تنقّل بين الصور
 * - نقر مزدوج = إعجاب (Like)
 */
@Composable
fun SwipeCardView(
    card: DiscoverCard,
    onSwiped: (SwipeDirection) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDoubleTap: () -> Unit = {},
    showOverlay: Boolean = true
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var offset by remember(card.id) { mutableStateOf(Offset.Zero) }
    var isGone by remember(card.id) { mutableStateOf(false) }
    var containerWidthPx by remember { mutableStateOf(1f) }
    var photoIndex by remember(card.id) { mutableIntStateOf(0) }

    val photos = card.galleryPhotos
    val swipeThresholdPx = with(density) { 120.dp.toPx() }
    val superThresholdPx = with(density) { 150.dp.toPx() }

    val animatedX by animateFloatAsState(
        targetValue = offset.x,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "swipeX"
    )
    val animatedY by animateFloatAsState(
        targetValue = offset.y,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "swipeY"
    )

    LaunchedEffect(isGone) {
        if (isGone) {
            kotlinx.coroutines.delay(280)
            val dir = when {
                offset.y < -superThresholdPx -> SwipeDirection.UP
                offset.x > 0 -> SwipeDirection.RIGHT
                else -> SwipeDirection.LEFT
            }
            onSwiped(dir)
        }
    }

    val rotation = (animatedX / 20f).coerceIn(-20f, 20f)
    // مغادرة البطاقة: تصغر وتتلاشى مع انزلاقها (iOS: scale 0.5 + opacity 0)
    val goneProgress by animateFloatAsState(
        targetValue = if (isGone) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "gone"
    )
    // ظلّ بلون الاتجاه أثناء السحب — أخضر/أحمر/أزرق
    val shadowColor = when {
        offset.y < -with(density) { 50.dp.toPx() } -> Color(0xFF2EA9FF)
        offset.x > with(density) { 50.dp.toPx() } -> Color(0xFF4CAF50)
        offset.x < -with(density) { 50.dp.toPx() } -> Color(0xFFFF5252)
        else -> Color.Black
    }
    val age = ProfileFormatter.computeAge(card.birthDate)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { containerWidthPx = it.size.width.toFloat().coerceAtLeast(1f) }
            .offset { IntOffset(animatedX.roundToInt(), animatedY.coerceAtMost(0f).roundToInt()) }
            .rotate(rotation)
            .graphicsLayer {
                val sc = 1f - 0.5f * goneProgress
                scaleX = sc; scaleY = sc
                alpha = 1f - goneProgress
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(24.dp))
            // خلفية صلبة خلف الصورة (iOS #1F1233) — لا بطاقة شفّافة أثناء التحميل
            .background(Brush.linearGradient(listOf(Color(0xFF3A2B5F), Color(0xFF1F1233))))
            .border(
                width = if (card.isPremium == true) 2.dp else 1.dp,
                brush = if (card.isPremium == true) Brush.linearGradient(
                    listOf(Color(0xFFFFD54F).copy(alpha = 0.6f), Color(0xFFFFA726).copy(alpha = 0.5f))
                ) else Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .pointerInput(enabled, card.id) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    },
                    onDragEnd = {
                        when {
                            offset.y < -superThresholdPx -> {
                                HapticHelper.medium(haptic)
                                offset = Offset(0f, -1500f)
                                isGone = true
                            }
                            offset.x > swipeThresholdPx -> {
                                HapticHelper.medium(haptic)
                                offset = Offset(containerWidthPx * 1.5f, offset.y)
                                isGone = true
                            }
                            offset.x < -swipeThresholdPx -> {
                                HapticHelper.medium(haptic)
                                offset = Offset(-containerWidthPx * 1.5f, offset.y)
                                isGone = true
                            }
                            else -> offset = Offset.Zero
                        }
                    },
                    onDragCancel = { offset = Offset.Zero }
                )
            }
            .pointerInput(enabled, card.id, photos.size) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        HapticHelper.medium(haptic)
                        onDoubleTap()
                    },
                    onTap = { pos ->
                        val width = containerWidthPx
                        when {
                            photos.size > 1 && pos.x < width * 0.33f -> {
                                HapticHelper.light(haptic)
                                photoIndex = (photoIndex - 1 + photos.size) % photos.size
                            }
                            photos.size > 1 && pos.x > width * 0.66f -> {
                                HapticHelper.light(haptic)
                                photoIndex = (photoIndex + 1) % photos.size
                            }
                            else -> onTap()
                        }
                    }
                )
            }
    ) {
        // Background image (pager)
        val photoUrl = photos.getOrNull(photoIndex)
        HalaAsyncImage(
            model = photoUrl,
            contentDescription = S.get(R.string.photo_of_named, card.name ?: S.get(R.string.label_user)),
            contentScale = ContentScale.Crop,
            fallbackName = card.name,
            modifier = Modifier.fillMaxSize()
        )

        // Photo indicator dashes (top)
        if (photos.size > 1 && showOverlay) {
            // كبسولات iOS: الحالية أعرض (20) والبقية 8، مع حركة عند التنقّل
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                photos.forEachIndexed { idx, _ ->
                    val active = idx == photoIndex
                    val w by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (active) 20.dp else 8.dp,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "dash"
                    )
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }

        if (showOverlay) {
            // Readability gradient
            // شريط القراءة في الـ 42% السفلية فقط (iOS readabilityLayer) — الصورة تبقى نقيّة أعلاه
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            InfoOverlay(
                card = card,
                age = age,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // Stamps
            val likeOpacity = (offset.x / 120f).coerceIn(0f, 1f)
            val nopeOpacity = (-offset.x / 120f).coerceIn(0f, 1f)
            val superOpacity = (-offset.y / 120f).coerceIn(0f, 1f)

            if (likeOpacity > 0f) Stamp(
                text = "LIKE",
                icon = Icons.Filled.Favorite,
                color = Color(0xFF4CAF50),
                rotation = -15f,
                alignment = Alignment.TopStart,
                opacity = likeOpacity
            )
            if (nopeOpacity > 0f) Stamp(
                text = "NOPE",
                icon = Icons.Filled.Close,
                color = Color(0xFFFF5252),
                rotation = 15f,
                alignment = Alignment.TopEnd,
                opacity = nopeOpacity
            )
            if (superOpacity > 0f) Stamp(
                text = "SUPER",
                icon = Icons.Filled.Star,
                color = Color(0xFF2EA9FF),
                rotation = 0f,
                alignment = Alignment.Center,
                opacity = superOpacity
            )
        }
    }
}

@Composable
private fun InfoOverlay(
    card: DiscoverCard,
    age: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    append(card.name ?: S.get(R.string.label_user))
                    if (age != null) append(S.get(R.string.separator_age, age.toString()))
                },
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (card.isPremium == true) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (card.isVerified == true) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Color(0xFF6AB7FF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // مؤشّرات: الحضور + بالقرب منك (≤30 كم).
        // ⚠️ الحضور من lastLogin مثل iOS (`smartPresence`): متصل < ساعتين (نبض)،
        //    «نشط قبل…» حتى 24س. isOnline وحده كان يُسقط من دخل قبل ساعة — وهو
        //    أرجح من يردّ على طلبك اليوم.
        val presence = presenceOf(card)
        val nearby = card.distance != null && card.distance <= 30.0
        if (presence != null || nearby) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (presence != null) PresencePill(presence)
                if (nearby) NearbyPill()
            }
        }

        val locationText = buildString {
            val c = com.chathala.hala.core.data.Countries.byCode(card.country)
            if (c != null) append("${c.flag} ${c.name}")
            else if (!card.country.isNullOrBlank()) append(card.country)
            card.distance?.let {
                if (isNotEmpty()) append(" • ")
                append(S.get(R.string.distance_km, it.toInt()))
            }
        }
        if (locationText.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = locationText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        card.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            Text(
                text = bio,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NearbyPill() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xCCE91E8C))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = S.get(R.string.discover_near_you),
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

/** null = لا حضور ظاهر (أقدم من 24س أو مخفي) · first = النص · second = متصل الآن */
private fun presenceOf(card: DiscoverCard): Pair<String, Boolean>? {
    val age = com.chathala.hala.feature.notifications.util.NotificationFormat.ageMillis(card.lastLogin)
    val hour = 3_600_000L
    return when {
        card.isOnline == true || (age != null && age < 2 * hour) -> S.get(R.string.status_online) to true
        age != null && age < 24 * hour -> S.get(
            R.string.discover_active_ago,
            com.chathala.hala.feature.notifications.util.NotificationFormat.timeAgo(card.lastLogin)
        ) to false
        else -> null
    }
}

@Composable
private fun PresencePill(presence: Pair<String, Boolean>) {
    val (label, online) = presence
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(1200),
            androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "ring"
    )
    val dotColor = if (online) Color(0xFF4CAF50) else Color(0xFFFFB300)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xCC000000))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(8.dp)) {
            if (online) {
                // نبض حول النقطة — «متصل الآن» حيّ لا ثابت
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = ring; scaleY = ring
                            alpha = (2.2f - ring) / 1.2f * 0.6f
                        }
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Stamp(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    rotation: Float,
    alignment: Alignment,
    opacity: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(alignment)
            .padding(30.dp)
            .alpha(opacity)
            .rotate(rotation)
            .border(4.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
