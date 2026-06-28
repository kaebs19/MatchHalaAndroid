package com.chathala.hala.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chathala.hala.R
import com.chathala.hala.ui.components.HalaTextField

/**
 * حوار تأكيد حذف الحساب.
 * @param requirePassword لو المستخدم سجّل بـ email/password يجب تمرير كلمة المرور.
 */
@Composable
fun DeleteAccountDialog(
    requirePassword: Boolean,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (password: String?) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_account_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.delete_account_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (requirePassword) {
                    Spacer(Modifier.height(14.dp))
                    HalaTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.delete_account_password_hint),
                        isPassword = true,
                        keyboardType = KeyboardType.Password,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && (!requirePassword || password.isNotBlank()),
                onClick = {
                    onConfirm(if (requirePassword) password else null)
                }
            ) {
                Text(
                    text = stringResource(R.string.delete_account_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_account_cancel))
            }
        }
    )
}
