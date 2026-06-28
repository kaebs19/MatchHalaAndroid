package com.chathala.hala.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.R

/** قوة كلمة المرور: 0 (فارغة) .. 4 (قوية جداً). */
private fun passwordScore(pw: String): Int {
    if (pw.isEmpty()) return 0
    var s = 0
    if (pw.length >= 6) s++
    if (pw.length >= 10) s++
    if (pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() }) s++
    if (pw.any { it.isDigit() } && pw.any { !it.isLetterOrDigit() }) s++
    return s.coerceIn(0, 4)
}

/**
 * شريط متحرّك يوضّح قوة كلمة المرور (٣ مستويات: ضعيفة/متوسطة/قوية).
 * يظهر فقط عندما يبدأ المستخدم بالكتابة.
 */
@Composable
fun PasswordStrengthBar(
    password: String,
    modifier: Modifier = Modifier
) {
    if (password.isEmpty()) return

    val score = passwordScore(password)
    // 3 مستويات: 1-2 ضعيفة، 3 متوسطة، 4 قوية
    val (label, color, fraction) = when {
        score <= 2 -> Triple(stringResource(R.string.pwd_strength_weak), Color(0xFFE53935), 0.33f)
        score == 3 -> Triple(stringResource(R.string.pwd_strength_medium), Color(0xFFFB8C00), 0.66f)
        else -> Triple(stringResource(R.string.pwd_strength_strong), Color(0xFF43A047), 1f)
    }

    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "pwd-fraction")
    val animatedColor by animateColorAsState(targetValue = color, label = "pwd-color")

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(animatedColor)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pwd_strength_label, label),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = animatedColor
            )
        }
    }
}
