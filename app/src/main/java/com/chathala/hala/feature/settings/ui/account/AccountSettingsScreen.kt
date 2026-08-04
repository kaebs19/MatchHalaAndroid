package com.chathala.hala.feature.settings.ui.account

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.feature.settings.ui.DeleteAccountDialog
import com.chathala.hala.feature.settings.ui.SettingsViewModel
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenViolations: () -> Unit = {},
    onOpenStanding: () -> Unit = {},
    onOpenRequests: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScaffold(
            title = S.get(R.string.settings_section_account_settings),
            onBack = onBack
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountRow(
                    icon = Icons.Filled.Block,
                    label = S.get(R.string.blocked_users_title),
                    onClick = onOpenBlockedUsers
                )

                val authProvider = user?.authProvider
                if (authProvider == null || authProvider == "app") {
                    AccountRow(
                        icon = Icons.Filled.Lock,
                        label = S.get(R.string.change_password_title),
                        onClick = onOpenChangePassword
                    )
                }

                AccountRow(
                    icon = Icons.Filled.Shield,
                    label = S.get(R.string.account_standing_title),
                    onClick = onOpenStanding
                )

                AccountRow(
                    icon = Icons.Filled.Warning,
                    label = S.get(R.string.violations_history_title),
                    onClick = onOpenViolations
                )

                AccountRow(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    label = S.get(R.string.my_requests_title),
                    onClick = onOpenRequests
                )

                AccountRow(
                    icon = Icons.Filled.DeleteForever,
                    label = S.get(R.string.settings_delete_account),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteDialog = true }
                )
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDeleteDialog) {
        val requirePassword = user?.authProvider == "app" || user?.authProvider == null
        DeleteAccountDialog(
            requirePassword = requirePassword,
            loading = deleting,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { password ->
                viewModel.deleteAccount(password) {
                    showDeleteDialog = false
                    onLoggedOut()
                }
            }
        )
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
