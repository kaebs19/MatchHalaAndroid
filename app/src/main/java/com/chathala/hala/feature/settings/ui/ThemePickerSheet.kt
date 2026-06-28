package com.chathala.hala.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.HalaApp
import com.chathala.hala.core.storage.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModel.Factory
    )
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentTheme by viewModel.theme.collectAsState(initial = AppTheme.SYSTEM)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.chathala.hala.R.string.settings_theme_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ThemeOption(
                icon = Icons.Filled.SettingsBrightness,
                label = androidx.compose.ui.res.stringResource(com.chathala.hala.R.string.settings_theme_system),
                selected = currentTheme == AppTheme.SYSTEM,
                onClick = { viewModel.setTheme(AppTheme.SYSTEM); onDismiss() }
            )
            ThemeOption(
                icon = Icons.Filled.LightMode,
                label = androidx.compose.ui.res.stringResource(com.chathala.hala.R.string.settings_theme_light),
                selected = currentTheme == AppTheme.LIGHT,
                onClick = { viewModel.setTheme(AppTheme.LIGHT); onDismiss() }
            )
            ThemeOption(
                icon = Icons.Filled.DarkMode,
                label = androidx.compose.ui.res.stringResource(com.chathala.hala.R.string.settings_theme_dark),
                selected = currentTheme == AppTheme.DARK,
                onClick = { viewModel.setTheme(AppTheme.DARK); onDismiss() }
            )
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
