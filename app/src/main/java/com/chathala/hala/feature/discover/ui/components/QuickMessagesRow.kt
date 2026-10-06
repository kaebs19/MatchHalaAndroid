package com.chathala.hala.feature.discover.ui.components

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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
import com.chathala.hala.core.data.Countries
import androidx.compose.runtime.remember

/**
 * صف رسائل سريعة قابل للتمرير أفقياً.
 *
 * النقر **يُدرج** النصّ في حقل الرسالة عبر [onPick] ولا يُرسل: طلب المحادثة
 * إجراء يراه الطرف الآخر ولا يمكن سحبه، فإرساله بلمسة واحدة عابرة كان يُنتج
 * طلبات بالخطأ. المستخدم يعدّل النصّ إن شاء ثم يضغط زرّ الإرسال.
 *
 * ⚠️ مطابق لـ iOS (`MessageInputView.quickMessages`): الشرائح الثابتة عبارات
 *    نمطية يُرسلها الجميع للجميع فيُدفن الطلب بين مئات مثله؛ المشتقّة من بروفايل
 *    المستلم تتصدّر لأنها تُثبت أن المُرسِل قرأ شيئاً عنه. الإيموجي جزء من الرسالة.
 */

private data class QuickMessage(val emoji: String, val text: String) {
    val message: String get() = "$text $emoji"
}

private fun quickMessagesFor(country: String?, interest: String?, isOnline: Boolean?): List<QuickMessage> {
    val items = mutableListOf<QuickMessage>()
    // ١) إشارة شخصية من البروفايل — country رمز ISO، والاسم من Countries
    Countries.byCode(country)?.let {
        items += QuickMessage("📍", S.get(R.string.quick_msg_country, it.name))
    }
    interest?.takeIf { it.isNotBlank() }?.let {
        items += QuickMessage("✨", S.get(R.string.quick_msg_interest, it))
    }
    if (isOnline == true) {
        items += QuickMessage("👋", S.get(R.string.quick_msg_online))
    }
    // ٢) العامة في الذيل لمن لا يجد ما يقوله — بلغة التطبيق وحدها
    items += QuickMessage("💬", S.get(R.string.quick_msg_get_to_know))
    items += QuickMessage("🌹", S.get(R.string.quick_msg_how_are_you))
    items += QuickMessage("👋", S.get(R.string.quick_msg_hi))
    return items
}

@Composable
fun QuickMessagesRow(
    country: String?,
    isOnline: Boolean?,
    selected: String,
    interest: String? = null,
    onPick: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = S.get(R.string.quick_messages_label),
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
            remember(country, interest, isOnline) { quickMessagesFor(country, interest, isOnline) }.forEach { msg ->
                QuickMessageChip(
                    emoji = msg.emoji,
                    text = msg.text,
                    selected = selected == msg.message,
                    onClick = {
                        if (!enabled) return@QuickMessageChip
                        HapticHelper.light(haptic)
                        onPick(msg.message)
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
