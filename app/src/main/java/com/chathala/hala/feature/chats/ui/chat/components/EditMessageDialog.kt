package com.chathala.hala.feature.chats.ui.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * تحرير نصّ رسالة مُرسَلة — ميزة اشتراك، ومهلتها 15 دقيقة يفرضها الخادم.
 */
@Composable
fun EditMessageDialog(
    initialContent: String,
    submitting: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember {
        mutableStateOf(
            TextFieldValue(initialContent, androidx.compose.ui.text.TextRange(initialContent.length))
        )
    }
    val text = value.text.trim()
    val changed = text.isNotBlank() && text != initialContent.trim()

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("تعديل الرسالة 👑") },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !submitting,
                    singleLine = false,
                    maxLines = 6
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "يمكن التعديل خلال ١٥ دقيقة من الإرسال، ويظهر للطرف الآخر وسم «مُعدَّلة».",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.text) },
                enabled = changed && !submitting
            ) {
                Text(if (submitting) "جارٍ الحفظ…" else "حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("إلغاء") }
        }
    )
}
