package com.chathala.hala.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import kotlinx.coroutines.delay

/** الخادم يُرسل `role` للمشرفين وحدهم — قيمته لغيرهم لا تصل أصلاً. */
fun isOfficialRole(role: String?): Boolean = role == "admin" || role == "superadmin"

/** لون الشارة الرسمية — ذهبي يميّزها عن علامة التوثيق الزرقاء العادية. */
private val OfficialGold = Color(0xFFF5B400)
private val OfficialGoldDeep = Color(0xFFE08E00)

/**
 * علامة الحساب الرسمي (المشرف): دائرة ذهبية بعلامة صح.
 *
 * الضغط عليها يُظهر فقاعة «حساب رسمي معتمد» فوقها وتختفي وحدها بعد ثوانٍ —
 * والضغط يُستهلك هنا فلا يصل للبطاقة/الصف تحتها.
 */
@Composable
fun OfficialBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    borderColor: Color? = null
) {
    var showTip by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(showTip) {
        if (showTip) {
            delay(2_500)
            showTip = false
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(OfficialGold, OfficialGoldDeep)))
                .then(
                    if (borderColor != null) Modifier.border(1.5.dp, borderColor, CircleShape)
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showTip = !showTip
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = S.get(R.string.official_account_verified),
                tint = Color.White,
                modifier = Modifier.size(size * 0.68f)
            )
        }

        if (showTip) {
            val gapPx = with(LocalDensity.current) { 6.dp.roundToPx() }
            Popup(
                popupPositionProvider = remember(gapPx) { AboveAnchor(gapPx) },
                onDismissRequest = { showTip = false },
                properties = PopupProperties(focusable = false)
            ) {
                OfficialTip()
            }
        }
    }
}

/** الفقاعة فوق الشارة ومتوسّطة عليها، وتنزل تحتها إن لم يتّسع الأعلى؛ لا تخرج عن الشاشة أفقياً. */
private class AboveAnchor(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val above = anchorBounds.top - gapPx - popupContentSize.height
        val y = if (above >= 0) above else anchorBounds.bottom + gapPx
        return IntOffset(x, y)
    }
}

@Composable
private fun OfficialTip() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.85f,
        animationSpec = tween(160),
        label = "official-tip"
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 6.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = scale; scaleY = scale
            alpha = scale
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = OfficialGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = S.get(R.string.official_account_verified),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}
