package com.chathala.hala.feature.chats.ui.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * صف قابل للسحب: يمين → تثبيت، يسار → حذف.
 * بعد التنفيذ يُعاد State إلى Settled لمنع الإخفاء.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableConversation(
    key: Any,
    isPinned: Boolean,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    key(key) {
        val state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> { onPin(); false }
                    SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        )

        LaunchedEffect(state.currentValue) {
            if (state.currentValue != SwipeToDismissBoxValue.Settled) {
                state.reset()
            }
        }

        SwipeToDismissBox(
            state = state,
            backgroundContent = {
                SwipeBackground(
                    isStartToEnd = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd,
                    isPinned = isPinned
                )
            },
            content = { content() }
        )
    }
}

@Composable
private fun SwipeBackground(isStartToEnd: Boolean, isPinned: Boolean) {
    val bg = if (isStartToEnd) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.error
    val alignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg.copy(alpha = 0.18f))
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = if (isStartToEnd) Icons.Filled.PushPin else Icons.Filled.DeleteOutline,
            contentDescription = null,
            tint = bg,
            modifier = Modifier.size(26.dp)
        )
    }
}
