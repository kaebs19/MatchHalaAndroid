package com.chathala.hala.feature.discover.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.core.util.HapticHelper

/**
 * صف رسائل سريعة قابل للتمرير أفقياً — مطابق iOS MessageInputView.
 * عند النقر: يُرسل النص مباشرة عبر [onSend] (يعمل كزر إرسال).
 */

private data class QuickMessage(val emoji: String, val text: String)

private val QuickMessages = listOf(
    QuickMessage("👋", "مرحبا"),
    QuickMessage("😊", "Hello"),
    QuickMessage("🌹", "كيف حالك؟"),
    QuickMessage("✨", "How are you?"),
    QuickMessage("☕️", "نتعرف؟"),
    QuickMessage("💬", "Let's chat!")
)

@Composable
fun QuickMessagesRow(
    onSend: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "رسائل سريعة (اضغط للإرسال)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickMessages.forEach { msg ->
                QuickMessageChip(
                    emoji = msg.emoji,
                    text = msg.text,
                    selected = false,
                    onClick = {
                        if (!enabled) return@QuickMessageChip
                        HapticHelper.light(haptic)
                        onSend(msg.text)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickMessageChip(
    emoji: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(
                width = if (selected) 0.dp else 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}
