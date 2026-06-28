package com.chathala.hala.feature.settings.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold
import com.chathala.hala.ui.components.FormError
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.HalaTextField

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ChangePasswordViewModel = viewModel(factory = ChangePasswordViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) {
            onSuccess()
            viewModel.clearSuccess()
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.change_password_title),
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HalaTextField(
                value = state.current,
                onValueChange = viewModel::setCurrent,
                label = stringResource(R.string.change_password_current),
                isPassword = true,
                keyboardType = KeyboardType.Password,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                isError = state.currentError != null,
                errorMessage = state.currentError
            )

            HalaTextField(
                value = state.new,
                onValueChange = viewModel::setNew,
                label = stringResource(R.string.change_password_new),
                isPassword = true,
                keyboardType = KeyboardType.Password,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                isError = state.newError != null,
                errorMessage = state.newError
            )
            if (state.newError == null) {
                Text(
                    text = stringResource(R.string.password_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            HalaTextField(
                value = state.confirm,
                onValueChange = viewModel::setConfirm,
                label = stringResource(R.string.change_password_confirm),
                isPassword = true,
                keyboardType = KeyboardType.Password,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                isError = state.confirmError != null,
                errorMessage = state.confirmError
            )

            FormError(state.error)

            Spacer(Modifier.height(8.dp))

            HalaPrimaryButton(
                text = stringResource(R.string.change_password_submit),
                loading = state.loading,
                onClick = { viewModel.submit() }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
