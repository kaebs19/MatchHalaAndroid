package com.chathala.hala.feature.chats.ui.chat.components

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val QUICK_REACTIONS = listOf("❤️", "😂", "👍", "😮", "😢", "🔥")

/** بند واحد في قائمة الرسالة. */
private data class MenuAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    /** ميزة اشتراك — يظهر بجانبها تاج، وتُوجَّه للاشتراك عند المستخدم المجاني */
    val premium: Boolean = false
)

/**
 * قائمة الضغط المطوّل على الرسالة بنمط iOS: شريط ردود فعل عائم فوق بطاقة إجراءات،
 * مثبّتة عند الرسالة نفسها بدل ورقة سفلية.
 *
 * تُرسَم داخل نافذة الشاشة نفسها (لا Popup/Dialog) تفادياً لاقتطاع النوافذ العائمة للحواف.
 *
 * @param anchor حدود فقاعة الرسالة في جذر الشاشة — تُثبَّت القائمة قربها.
 */
@Composable
fun MessageContextMenu(
    anchor: Rect?,
    canCopy: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    isPremium: Boolean,
    onPick: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onPremiumRequired: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val appear by animateFloatAsState(targetValue = 1f, label = "menu-appear")

    val actions = buildList {
        add(MenuAction(S.get(R.string.action_reply), Icons.AutoMirrored.Filled.Reply, onReply))
        if (canCopy) add(MenuAction(S.get(R.string.action_copy), Icons.Filled.ContentCopy, onCopy))
        add(MenuAction(S.get(R.string.action_forward), Icons.AutoMirrored.Filled.Send, onForward))
        if (canEdit) add(MenuAction(S.get(R.string.action_edit), Icons.Filled.Edit, onEdit, premium = true))
        if (canDelete) {
            add(MenuAction(S.get(R.string.action_delete), Icons.Filled.Delete, onDelete, destructive = true, premium = true))
        }
        add(MenuAction(S.get(R.string.action_report), Icons.Filled.ReportProblem, onReport, destructive = true))
    }

    // ارتفاع تقريبي للقائمة (شريط الردود + البطاقة) لاختيار موضعها فوق/تحت الفقاعة
    val menuHeightPx = with(density) { (72 + actions.size * 54 + 24).dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * appear))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        val screenHeightPx = with(density) { maxHeightPx() }
        val topPx = when {
            anchor == null -> (screenHeightPx - menuHeightPx) / 2f
            anchor.bottom + menuHeightPx + marginPx < screenHeightPx -> anchor.bottom + marginPx
            anchor.top - menuHeightPx - marginPx > 0f -> anchor.top - menuHeightPx - marginPx
            else -> (screenHeightPx - menuHeightPx) / 2f
        }.coerceIn(marginPx, (screenHeightPx - menuHeightPx - marginPx).coerceAtLeast(marginPx))

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, topPx.roundToInt()) }
                .alpha(appear)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── شريط ردود الفعل ──
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPick(emoji)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── بطاقة الإجراءات ──
            Surface(
                modifier = Modifier.width(280.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    actions.forEachIndexed { index, action ->
                        val locked = action.premium && !isPremium
                        ActionRow(
                            action = action,
                            locked = locked,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (locked) onPremiumRequired() else action.onClick()
                            }
                        )
                        if (index != actions.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/** ارتفاع الشاشة بالبكسل من إعدادات الجهاز. */
@Composable
private fun androidx.compose.ui.unit.Density.maxHeightPx(): Float {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.screenHeightDp.dp.toPx()
}

@Composable
private fun ActionRow(
    action: MenuAction,
    locked: Boolean,
    onClick: () -> Unit
) {
    val contentColor = when {
        action.destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val alpha = if (locked) 0.45f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // الترتيب مثل iOS: النصّ عند الحافة البادئة والأيقونة عند المقابلة
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = contentColor.copy(alpha = alpha),
            modifier = Modifier.weight(1f)
        )
        if (action.premium) {
            // تاج الاشتراك — يبقى ظاهراً للمشترك أيضاً ليُعرف أنها ميزة مدفوعة
            Text(text = "👑", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(10.dp))
        }
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = alpha),
            modifier = Modifier.size(22.dp)
        )
    }
}
