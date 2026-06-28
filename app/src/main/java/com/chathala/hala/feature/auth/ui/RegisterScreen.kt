package com.chathala.hala.feature.auth.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chathala.hala.ui.components.PasswordStrengthBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.ui.components.AuthFooterLinks
import com.chathala.hala.ui.components.AuthScaffold
import com.chathala.hala.ui.components.FormError
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.HalaTextField
import com.chathala.hala.feature.auth.ui.components.TermsAgreementRow
import com.chathala.hala.core.util.Validators

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onBanned: (com.chathala.hala.feature.suspension.data.SuspensionMode) -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        focusManager.clearFocus()
        nameError = Validators.name(name)
        emailError = Validators.email(email)
        passwordError = Validators.password(password)
        confirmError = Validators.passwordConfirm(password, confirm)
        termsError = if (!termsAccepted) "يجب الموافقة على الشروط وسياسة الخصوصية" else null
        if (nameError == null && emailError == null && passwordError == null &&
            confirmError == null && termsError == null
        ) {
            viewModel.register(name.trim(), email.trim(), password)
        }
    }

    LaunchedEffect(state.done) {
        if (state.done) {
            onSuccess()
            viewModel.resetFeedback()
        }
    }

    LaunchedEffect(state.bannedMode) {
        state.bannedMode?.let {
            onBanned(it)
            viewModel.resetFeedback()
        }
    }

    AuthScaffold(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        onBack = onBackToLogin
    ) {
        HalaTextField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = stringResource(R.string.field_name),
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            isError = nameError != null,
            errorMessage = nameError,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(12.dp))
        HalaTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = stringResource(R.string.field_email),
            keyboardType = KeyboardType.Email,
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            isError = emailError != null,
            errorMessage = emailError,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(12.dp))
        HalaTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = stringResource(R.string.field_password),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            isError = passwordError != null,
            errorMessage = passwordError,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        // مؤشّر قوة كلمة المرور (يظهر عند الكتابة)، أو التلميح إن كانت فارغة
        if (password.isNotEmpty()) {
            PasswordStrengthBar(password = password)
        } else if (passwordError == null) {
            androidx.compose.material3.Text(
                text = stringResource(R.string.password_hint),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        HalaTextField(
            value = confirm,
            onValueChange = { confirm = it; confirmError = null },
            label = stringResource(R.string.field_confirm_password),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            isError = confirmError != null,
            errorMessage = confirmError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() })
        )

        Spacer(Modifier.height(12.dp))
        TermsAgreementRow(
            accepted = termsAccepted,
            onToggle = { termsAccepted = it; if (it) termsError = null },
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy
        )
        FormError(termsError)
        FormError(state.error)

        Spacer(Modifier.height(16.dp))

        HalaPrimaryButton(
            text = stringResource(R.string.btn_register),
            loading = state.loading,
            onClick = { submit() }
        )

        Spacer(Modifier.height(24.dp))

        AuthFooterLinks(
            prompt = stringResource(R.string.have_account),
            actionText = stringResource(R.string.go_to_login),
            onAction = onBackToLogin
        )
    }
}
