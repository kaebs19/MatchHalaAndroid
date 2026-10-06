package com.chathala.hala.feature.discover.ui.components

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.core.util.HapticHelper
import com.chathala.hala.feature.discover.data.DiscoverCard
import com.chathala.hala.ui.components.HalaAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * نافذة «تطابق!» — مطابقة iOS `MatchPopupView`.
 * دخول متتابع: القلب (spring) ← العنوان ← الصورتان من الجانبين ← الأزرار،
 * مع قلوب متطايرة. النقر خارج المحتوى أو الرجوع = «تابع الاستكشاف».
 */
@Composable
fun MatchPopup(
    matched: DiscoverCard,
    myAvatar: String?,
    onSendMessage: () -> Unit,
    onKeepSwiping: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val heart = remember { Animatable(0f) }
    val content = remember { Animatable(0f) }
    val avatars = remember { Animatable(0f) }
    val buttons = remember { Animatable(0f) }
    val burst = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }

    BackHandler(onBack = onKeepSwiping)

    LaunchedEffect(matched.id) {
        HapticHelper.medium(haptic)
        launch { delay(100); heart.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
        launch { delay(300); content.animateTo(1f, tween(500)) }
        launch { delay(500); avatars.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) }
        launch { delay(800); buttons.animateTo(1f, tween(400)) }
        delay(200); burst.tryEmit(Unit)
        delay(500); burst.tryEmit(Unit)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onKeepSwiping
            ),
        contentAlignment = Alignment.Center
    ) {
        FloatingHearts(trigger = burst)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                // يمتصّ النقر داخل المحتوى فلا يُغلق النافذة
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary

            // القلب مع توهّج
            Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(heart.value)) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(1.2f)
                        .background(
                            Brush.radialGradient(listOf(primary.copy(alpha = 0.4f), Color.Transparent)),
                            CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(content.value)
                    .offset(y = (20 * (1 - content.value)).dp)
            ) {
                Text(
                    text = S.get(R.string.match_title),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = S.get(R.string.match_subtitle, matched.name ?: S.get(R.string.label_user)),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy((-20).dp)) {
                MatchAvatar(
                    url = myAvatar,
                    border = primary,
                    modifier = Modifier
                        .offset(x = (-50 * (1 - avatars.value)).dp)
                        .alpha(avatars.value)
                )
                MatchAvatar(
                    url = matched.profileImage,
                    name = matched.name,
                    border = secondary,
                    modifier = Modifier
                        .offset(x = (50 * (1 - avatars.value)).dp)
                        .alpha(avatars.value)
                )
            }

            Text(
                text = S.get(R.string.match_hint),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(buttons.value)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.alpha(buttons.value)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(primary, secondary)))
                        .clickable(enabled = buttons.value > 0.5f, onClick = onSendMessage)
                        .padding(vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = S.get(R.string.match_send_message),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = S.get(R.string.match_keep_swiping),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable(enabled = buttons.value > 0.5f, onClick = onKeepSwiping)
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun MatchAvatar(
    url: String?,
    border: Color,
    modifier: Modifier = Modifier,
    name: String? = null
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .clip(CircleShape)
            .border(3.dp, border, CircleShape)
            .background(Color.Gray.copy(alpha = 0.3f))
    ) {
        HalaAsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            fallbackName = name,
            modifier = Modifier.fillMaxSize()
        )
    }
}
