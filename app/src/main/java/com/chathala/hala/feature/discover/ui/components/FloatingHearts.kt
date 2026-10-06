package com.chathala.hala.feature.discover.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * قلوب متطايرة عند الإعجاب — مطابقة iOS `FloatingHeartsView`:
 * 6–10 قلوب بأحجام وألوان وانحراف أفقي عشوائي، تصعد وتتلاشى خلال ~1.4ث.
 * لا تلتقط اللمس.
 */
private data class Heart(
    val id: Long,
    val xFraction: Float,
    val size: Dp,
    val color: Color,
    val delayMs: Int,
    val durationMs: Int,
    val drift: Dp
)

private val HeartColors = listOf(
    Color(0xFFE91E8C), Color(0xFFFF69B4), Color(0xCCF44336),
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xB3FF69B4)
)

@Composable
fun FloatingHearts(trigger: Flow<Unit>, modifier: Modifier = Modifier) {
    val hearts = remember { mutableStateListOf<Heart>() }

    LaunchedEffect(trigger) {
        trigger.collect {
            val batch = List(Random.nextInt(6, 11)) {
                Heart(
                    id = System.nanoTime() + it,
                    xFraction = Random.nextFloat() * 0.6f + 0.2f,
                    size = Random.nextInt(14, 29).dp,
                    color = HeartColors.random(),
                    delayMs = Random.nextInt(0, 300),
                    durationMs = Random.nextInt(800, 1400),
                    drift = Random.nextInt(-40, 41).dp
                )
            }
            hearts += batch
            launch {
                delay(1_800)
                hearts.removeAll(batch)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        hearts.forEach { heart ->
            androidx.compose.runtime.key(heart.id) {
                HeartParticle(heart, w, h)
            }
        }
    }
}

@Composable
private fun HeartParticle(heart: Heart, width: Dp, height: Dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(heart.delayMs.toLong())
        progress.animateTo(1f, tween(heart.durationMs, easing = FastOutSlowInEasing))
    }
    val p = progress.value
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = heart.color,
            modifier = Modifier
                .offset(
                    x = width * heart.xFraction + heart.drift * p,
                    // تبدأ قرب منتصف الشاشة السفلي وتصعد ~45% من الارتفاع
                    y = height * 0.62f - height * 0.45f * p
                )
                .size(heart.size)
                .scale(0.6f + 0.6f * p)
                .alpha(if (p == 0f) 0f else 1f - p)
        )
    }
}
