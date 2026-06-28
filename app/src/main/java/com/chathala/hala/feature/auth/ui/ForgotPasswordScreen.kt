package com.chathala.hala.feature.auth.ui

import android.util.Patterns
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
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
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onCodeSent: (String) -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.done) {
        if (state.done) {
            onCodeSent(email.trim())
            viewModel.resetFeedback()
        }
    }

    AuthScaffold(
        title = stringResource(R.string.forgot_password_title),
        subtitle = stringResource(R.string.forgot_password_subtitle),
        onBack = onBack
    ) {
        HalaTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = stringResource(R.string.field_email),
            keyboardType = KeyboardType.Email,
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            isError = emailError != null,
            errorMessage = emailError
        )

        FormError(state.error)

        Spacer(Modifier.height(24.dp))

        HalaPrimaryButton(
            text = stringResource(R.string.btn_send_code),
            loading = state.loading,
            onClick = {
                val emailLocal = email.trim()
                emailError = when {
                    emailLocal.isEmpty() -> "البريد الإلكتروني مطلوب"
                    !Patterns.EMAIL_ADDRESS.matcher(emailLocal).matches() -> "البريد الإلكتروني غير صحيح"
                    else -> null
                }
                if (emailError == null) {
                    viewModel.forgotPassword(emailLocal)
                }
            }
        )
    }
}
