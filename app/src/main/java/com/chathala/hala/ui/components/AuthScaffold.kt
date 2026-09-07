package com.chathala.hala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.chathala.hala.core.config.LogoConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    /** يعرض زر تبديل اللغة في أعلى الهيدر — لشاشات الدخول قبل توفّر الإعدادات. */
    showLanguageToggle: Boolean = false,
    headerExtra: (@Composable () -> Unit)? = null,
    /** يُثبَّت أسفل الشاشة خارج منطقة التمرير — لسطر الشروط والخصوصية. */
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // ظهور تدريجي للنموذج عند فتح الشاشة (تلاشٍ + انزلاق لأعلى)
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val formAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "form-alpha"
    )
    val formOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 60f,
        animationSpec = tween(durationMillis = 450),
        label = "form-offset"
    )

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // ── الهيدر: خلفية الثيم نفسها بلا متدرّج، الشعار هو البطل ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                if (showLanguageToggle) {
                    LanguageToggleChip(modifier = Modifier.align(Alignment.TopEnd))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthLogoBadge()
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (headerExtra != null) {
                        Spacer(Modifier.height(6.dp))
                        headerExtra()
                    }
                }
            }

            // ── النموذج (مع ظهور تدريجي) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(formAlpha)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, formOffset.toInt())
                        }
                    }
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 24.dp)
            ) {
                content()
            }
        }

        if (footer != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                footer()
            }
        }
    }
}

/**
 * شارة الشعار: الشعار كبيراً داخل دائرة بحلقة وردية رفيعة وظلّ ناعم — يحمل الهوية
 * بعدما زال المتدرّج، ويعمل على الأبيض والأسود معاً.
 */
@Composable
private fun AuthLogoBadge() {
    val ring = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(148.dp)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = ring.copy(alpha = 0.35f),
                spotColor = ring.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, ring.copy(alpha = 0.55f), CircleShape)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val logoRes = LogoConfig.defaultLogoRes
        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
