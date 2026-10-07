package com.chathala.hala.feature.main.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chathala.hala.R
import com.chathala.hala.core.ads.findActivity
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.review.AppRating
import kotlinx.coroutines.delay

/**
 * طلب تقييم مهذّب يظهر نادراً — قواعد التوقيت كلها في [AppRating].
 *
 * النص محايد عمداً: سياسة Google تمنع سؤال المستخدم عن رأيه («هل أعجبك؟») قبل
 * نافذة التقييم، فنكتفي بدعوة لطيفة وثلاثة خيارات واضحة.
 */
@Composable
fun RateAppPrompt() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppRating.onMainScreenEntered()
        // مهلة قصيرة بعد العودة حتى تستقرّ الشاشة ولا يبدو الطلب مفاجئاً
        delay(1_200)
        if (AppRating.shouldPrompt()) {
            AppRating.markPrompted()
            visible = true
        }
    }

    if (!visible) return

    RateDialog(
        onRate = {
            visible = false
            context.findActivity()?.let(AppRating::launchReview)
        },
        onLater = {
            visible = false
            AppRating.onLater()
        },
        onNever = {
            visible = false
            AppRating.onNever()
        }
    )
}

@Composable
private fun RateDialog(onRate: () -> Unit, onLater: () -> Unit, onNever: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.9f,
        animationSpec = tween(220),
        label = "rate-prompt-scale"
    )

    Dialog(onDismissRequest = onLater) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // هيدر متدرّج بهوية التطبيق + خمس نجوم
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { i ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                // النجمة الوسطى أكبر قليلاً — قوس لطيف
                                modifier = Modifier.size(if (i == 2) 40.dp else if (i == 1 || i == 3) 34.dp else 28.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = S.get(R.string.rate_prompt_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = S.get(R.string.rate_prompt_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = onRate,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = S.get(R.string.rate_prompt_rate),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onLater) {
                        Text(
                            text = S.get(R.string.rate_prompt_later),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onNever) {
                        Text(
                            text = S.get(R.string.rate_prompt_never),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
