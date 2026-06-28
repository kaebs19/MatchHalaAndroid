package com.chathala.hala.feature.auth.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.ui.components.AuthScaffold
import com.chathala.hala.ui.components.FormError
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.HalaTextField

@Composable
fun ResetPasswordScreen(
    email: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.done) {
        if (state.done) {
            onSuccess()
            viewModel.resetFeedback()
        }
    }

    AuthScaffold(
        title = stringResource(R.string.reset_password_title),
        subtitle = stringResource(R.string.reset_password_subtitle),
        onBack = onBack,
        headerExtra = {
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        HalaTextField(
            value = code,
            onValueChange = { code = it; codeError = null },
            label = stringResource(R.string.field_reset_code),
            keyboardType = KeyboardType.Number,
            leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null) },
            isError = codeError != null,
            errorMessage = codeError
        )
        Spacer(Modifier.height(12.dp))
        HalaTextField(
            value = newPassword,
            onValueChange = { newPassword = it; passwordError = null },
            label = stringResource(R.string.field_new_password),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            isError = passwordError != null,
            errorMessage = passwordError
        )
        Spacer(Modifier.height(12.dp))
        HalaTextField(
            value = confirm,
            onValueChange = { confirm = it; confirmError = null },
            label = stringResource(R.string.field_confirm_password),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            isError = confirmError != null,
            errorMessage = confirmError
        )

        FormError(state.error)

        Spacer(Modifier.height(24.dp))

        HalaPrimaryButton(
            text = stringResource(R.string.btn_reset_password),
            loading = state.loading,
            onClick = {
                codeError = if (code.trim().isEmpty()) "رمز التحقق مطلوب" else null
                passwordError = if (newPassword.length < 6) "كلمة المرور يجب أن تكون 6 أحرف على الأقل" else null
                confirmError = if (newPassword != confirm) "كلمتا المرور غير متطابقتين" else null

                if (codeError == null && passwordError == null && confirmError == null) {
                    viewModel.resetPassword(email, code.trim(), newPassword)
                }
            }
        )
    }
}
