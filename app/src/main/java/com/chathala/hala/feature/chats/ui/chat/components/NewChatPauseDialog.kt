package com.chathala.hala.feature.chats.ui.chat.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.RestrictionCenter
import androidx.compose.ui.window.DialogProperties

/**
 * ⏸ حوار إيقاف بدء المحادثات الجديدة (الحسابات الخارجية) — يُركَّب مرة واحدة في جذر
 * التطبيق فيعلو أي شاشة، ويبقى حتى «حسناً» (مقابل `NewChatPauseAlert` في iOS).
 */
@Composable
fun NewChatPauseDialogHost() {
    val until by RestrictionCenter.newChatPauseUntil.collectAsStateWithLifecycle()
    val end = until ?: return
    AlertDialog(
        onDismissRequest = RestrictionCenter::dismissNewChatPause,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(S.get(R.string.new_chat_pause_title), fontWeight = FontWeight.Bold) },
        text = { Text(S.get(R.string.new_chat_pause_body, RestrictionCenter.formatRemaining(end))) },
        confirmButton = {
            TextButton(onClick = RestrictionCenter::dismissNewChatPause) {
                Text(S.get(R.string.action_ok_plain), fontWeight = FontWeight.Bold)
            }
        }
    )
}
