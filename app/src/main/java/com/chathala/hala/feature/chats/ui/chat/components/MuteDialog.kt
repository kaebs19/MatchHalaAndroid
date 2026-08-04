package com.chathala.hala.feature.chats.ui.chat.components

import com.chathala.hala.R
import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** خيارات مدة الكتم. */
enum class MuteDuration(private val labelRes: Int, val millis: Long?) {
    HOUR(R.string.mute_one_hour, 60L * 60 * 1000),
    EIGHT_HOURS(R.string.mute_eight_hours, 8L * 60 * 60 * 1000),
    DAY(R.string.mute_one_day, 24L * 60 * 60 * 1000),
    WEEK(R.string.mute_one_week, 7L * 24 * 60 * 60 * 1000),
    FOREVER(R.string.mute_forever, null);

    /** يُقرأ وقت العرض — ثوابت الـ enum تُهيّأ مرة واحدة فقط. */
    val label: String get() = S.get(labelRes)
}

@Composable
fun MuteDialog(
    onDismiss: () -> Unit,
    onConfirm: (mutedUntilIso: String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(S.get(R.string.chat_mute_notifications))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = S.get(R.string.mute_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                MuteDuration.entries.forEach { opt ->
                    Text(
                        text = opt.label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val iso = opt.millis?.let { ms ->
                                    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                                    fmt.format(Date(System.currentTimeMillis() + ms))
                                }
                                onConfirm(iso)
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(S.get(R.string.action_cancel)) }
        }
    )
}
