package com.chathala.hala.feature.stories.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * خلفيات القصة النصّية — **بهذا الترتيب بالضبط** (الخادم ولوحة التحكم يعرضانها بالفهرس).
 * تدرّج قطري من أعلى-بداية إلى أسفل-نهاية.
 */
val StoryBackgrounds: List<Pair<Color, Color>> = listOf(
    Color(0xFF833AB4) to Color(0xFFE1306C),
    Color(0xFFF58529) to Color(0xFFDD2A7B),
    Color(0xFF0F2027) to Color(0xFF2C5364),
    Color(0xFF11998E) to Color(0xFF38EF7D),
    Color(0xFFFC466B) to Color(0xFF3F5EFB),
    Color(0xFF000000) to Color(0xFF434343),
    Color(0xFFF7971E) to Color(0xFFFFD200),
    Color(0xFF8E2DE2) to Color(0xFF4A00E0)
)

@Composable
fun storyBackgroundBrush(index: Int?): Brush {
    val (a, b) = StoryBackgrounds[(index ?: 0).coerceIn(0, StoryBackgrounds.lastIndex)]
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // «أعلى-بداية»: يمين الشاشة في العربية
    return if (rtl) {
        Brush.linearGradient(listOf(a, b), start = Offset(Float.POSITIVE_INFINITY, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))
    } else {
        Brush.linearGradient(listOf(a, b), start = Offset.Zero, end = Offset.Infinite)
    }
}

/** قصة نصّية على خلفيتها — للعارض (حجم كامل) والبطاقات المصغّرة. */
@Composable
fun TextStoryCanvas(
    text: String,
    background: Int?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    maxLines: Int = 9,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier.background(storyBackgroundBrush(background)), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            lineHeight = fontSize * 1.25f,
            modifier = Modifier.padding(horizontal = if (fontSize.value > 14f) 28.dp else 6.dp)
        )
        content()
    }
}
