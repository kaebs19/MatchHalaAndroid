package com.chathala.hala.feature.auth.ui

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.feature.auth.data.FacebookSignInHelper
import com.chathala.hala.feature.auth.data.FacebookSignInResult
import com.chathala.hala.feature.auth.data.GoogleSignInHelper
import com.chathala.hala.feature.auth.data.GoogleSignInResult
import com.chathala.hala.ui.components.AuthFooterLinks
import com.chathala.hala.ui.components.AuthScaffold
import com.chathala.hala.ui.components.FormError
import com.chathala.hala.ui.components.FacebookSignInButton
import com.chathala.hala.ui.components.GoogleSignInButton
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.HalaTextField
import com.chathala.hala.ui.components.OrDivider
import com.chathala.hala.feature.legal.ui.components.TermsFooterLinks
import com.chathala.hala.ui.components.TextLink
import com.chathala.hala.core.util.Validators
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onBanned: (com.chathala.hala.feature.suspension.data.SuspensionMode) -> Unit,
    onSuccess: () -> Unit,
    onNewUser: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val googleHelper = remember { GoogleSignInHelper(context) }
    val facebookHelper = remember { FacebookSignInHelper(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        focusManager.clearFocus()
        emailError = Validators.email(email)
        passwordError = Validators.password(password)
        if (emailError == null && passwordError == null) {
            viewModel.login(email.trim(), password)
        }
    }

    LaunchedEffect(state.done) {
        if (state.done) {
            // أول دخول عبر Google/Apple → شاشة الترحيب ثم إكمال الملف؛ الحساب القائم → الرئيسية مباشرة.
            if (state.isNewUser) onNewUser() else onSuccess()
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
        title = S.get(R.string.login_welcome),
        subtitle = S.get(R.string.login_subtitle),
        showLanguageToggle = true
    ) {
        HalaTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = S.get(R.string.field_email),
            keyboardType = KeyboardType.Email,
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            isError = emailError != null,
            errorMessage = emailError,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(14.dp))
        HalaTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = S.get(R.string.field_password),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            isError = passwordError != null,
            errorMessage = passwordError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() })
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextLink(text = S.get(R.string.btn_forgot_password), onClick = onForgot)
        }

        FormError(state.error)

        Spacer(Modifier.height(20.dp))

        HalaPrimaryButton(
            text = S.get(R.string.btn_login),
            loading = state.loading,
            onClick = { submit() }
        )

        Spacer(Modifier.height(20.dp))
        OrDivider()
        Spacer(Modifier.height(20.dp))

        GoogleSignInButton(
            loading = state.loading,
            onClick = {
                scope.launch {
                    when (val r = googleHelper.signIn()) {
                        is GoogleSignInResult.Success -> viewModel.googleLogin(r.idToken)
                        is GoogleSignInResult.Error -> viewModel.setError(r.message)
                        GoogleSignInResult.Cancelled -> { /* user cancelled — do nothing */ }
                    }
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        FacebookSignInButton(
            loading = state.loading,
            onClick = {
                scope.launch {
                    when (val r = facebookHelper.signIn()) {
                        is FacebookSignInResult.Success -> viewModel.facebookLogin(r.accessToken)
                        is FacebookSignInResult.Error -> viewModel.setError(r.message)
                        FacebookSignInResult.Cancelled -> { /* user cancelled — do nothing */ }
                    }
                }
            }
        )

        Spacer(Modifier.height(28.dp))

        AuthFooterLinks(
            prompt = S.get(R.string.no_account),
            actionText = S.get(R.string.create_account),
            onAction = onRegister
        )

        Spacer(Modifier.height(20.dp))

        TermsFooterLinks(
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy
        )
    }
}
