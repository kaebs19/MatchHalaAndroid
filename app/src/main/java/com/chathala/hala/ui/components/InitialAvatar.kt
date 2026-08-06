package com.chathala.hala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * بديل الصورة حين لا يملك المستخدم صورة شخصية: الحرف الأول من اسمه على
 * تدرّج لوني ثابت مشتقّ من الاسم — أفضل من أيقونة «صورة معطوبة» التي كانت
 * تُوحي بعطل، وأوضح من صورة رمادية موحّدة لأن اللون يميّز المستخدمين بصرياً.
 *
 * يملأ الحيّز المعطى له، فالشكل (دائرة/مربّع) يأتي من `Modifier.clip` عند المُستدعي.
 */
@Composable
fun InitialAvatar(
    name: String?,
    modifier: Modifier = Modifier
) {
    val initial = name.firstLetterOrNull() ?: DEFAULT_INITIAL
    val colors = paletteFor(name)

    BoxWithConstraints(
        modifier = modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        // الحرف يتناسب مع حجم الدائرة: نفس المكوّن يخدم أفاتار 32dp و120dp.
        val side = minOf(maxWidth.value, maxHeight.value)
        Text(
            text = initial.toString(),
            color = Color.White,
            fontSize = (side * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** الشكل الدائري الجاهز — للمواضع التي لا تحتاج تخصيصاً. */
@Composable
fun InitialAvatarCircle(name: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        InitialAvatar(name = name, modifier = Modifier.fillMaxSize())
    }
}

private const val DEFAULT_INITIAL = '؟'

/**
 * أول حرف *ذي معنى*: نتخطّى المسافات والرموز والإيموجي، فكثير من الأسماء
 * تبدأ بها ("★ سارة") وكان الحرف الأول سيخرج رمزاً لا يدلّ على شيء.
 */
private fun String?.firstLetterOrNull(): Char? =
    this?.trim()?.firstOrNull { it.isLetter() || it.isDigit() }?.uppercaseChar()

/** تدرّجات ثابتة لكل اسم — نفس المستخدم يظهر بنفس اللون في كل الشاشات. */
private fun paletteFor(name: String?): List<Color> {
    val key = name?.trim().orEmpty()
    val index = if (key.isEmpty()) 0 else (key.hashCode().mod(PALETTES.size))
    return PALETTES[index]
}

private val PALETTES = listOf(
    listOf(Color(0xFFE91E63), Color(0xFF9C27B0)),
    listOf(Color(0xFF7E57C2), Color(0xFF5C6BC0)),
    listOf(Color(0xFF26A69A), Color(0xFF66BB6A)),
    listOf(Color(0xFFEF6C00), Color(0xFFF9A825)),
    listOf(Color(0xFF42A5F5), Color(0xFF26C6DA)),
    listOf(Color(0xFFAB47BC), Color(0xFFEC407A)),
    listOf(Color(0xFF5C6BC0), Color(0xFF29B6F6)),
    listOf(Color(0xFFD81B60), Color(0xFFF06292))
)
