package com.chathala.hala.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.core.storage.AppTheme
import com.chathala.hala.core.util.showToast
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenDiscoverSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    onOpenContentSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)

    var showThemeSheet by remember { mutableStateOf(false) }
    val snackbarHost = rememberHalaSnackbarHost()

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(onBack = onBack)
            Spacer(Modifier.height(8.dp))

            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.settings_appearance_mode),
                    value = when (theme) {
                        AppTheme.SYSTEM -> stringResource(R.string.settings_theme_system)
                        AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
                        AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
                    },
                    onClick = { showThemeSheet = true }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.notif_settings_title),
                    onClick = onOpenNotificationSettings
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_discover)) {
                SettingsItem(
                    icon = Icons.Filled.Explore,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.discover_settings_title),
                    onClick = onOpenDiscoverSettings
                )
            }

            SettingsSection(title = "تفضيلات المحتوى") {
                SettingsItem(
                    icon = Icons.Filled.VisibilityOff,
                    iconTint = Color(0xFFE91E8C),
                    label = "المحتوى الحساس",
                    onClick = onOpenContentSettings
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_privacy_security)) {
                SettingsItem(
                    icon = Icons.Filled.PrivacyTip,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.privacy_title),
                    onClick = onOpenPrivacySettings
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_account_settings)) {
                SettingsItem(
                    icon = Icons.Filled.ManageAccounts,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.settings_section_account_settings),
                    trailing = { AccountStatusBadge() },
                    onClick = onOpenAccountSettings
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_docs)) {
                SettingsItem(
                    icon = Icons.Filled.Description,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.terms_title),
                    onClick = onOpenTerms
                )
                SettingsItem(
                    icon = Icons.Filled.Shield,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.privacy_policy_doc),
                    onClick = onOpenPrivacy
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.about_title),
                    onClick = onOpenAbout
                )
                SettingsItem(
                    icon = Icons.Filled.ContactSupport,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.contact_title),
                    onClick = onOpenContact
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_account)) {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconTint = MaterialTheme.colorScheme.error,
                    label = stringResource(R.string.settings_logout),
                    onClick = { viewModel.logout(onLoggedOut) }
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showThemeSheet) {
        ThemePickerSheet(onDismiss = { showThemeSheet = false })
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

/** شارة حالة الحساب — تجلب الحالة بنفسها وتعرض نقطة ملوّنة + نص مختصر. */
@Composable
private fun AccountStatusBadge() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.chathala.hala.HalaApp
    val standing by androidx.compose.runtime.produceState<String?>(initialValue = null) {
        value = runCatching {
            val token = app.tokenStorage.token.firstOrNull() ?: return@runCatching null
            com.chathala.hala.core.network.ApiClient.service
                .getAccountStanding("Bearer $token").data?.standing
        }.getOrNull()
    }

    val st = standing ?: return
    if (st == "good") return // لا نعرض شيئاً عند السلامة
    val (color, label) = when (st) {
        "warning" -> Color(0xFFFFB300) to "تحذير"
        "restricted" -> Color(0xFFFF8C00) to "مقيّد"
        "suspended" -> Color(0xFFE53935) to "معلّق"
        else -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = color
        )
    }
}
