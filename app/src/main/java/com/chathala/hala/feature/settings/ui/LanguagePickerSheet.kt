package com.chathala.hala.feature.settings.ui

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.LocaleManager
import com.chathala.hala.core.storage.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModel.Factory
    )
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentLanguage by viewModel.language.collectAsState(initial = LocaleManager.current)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = S.get(R.string.settings_language_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            AppLanguage.entries.forEach { lang ->
                LanguageOption(
                    // أسماء اللغات تبقى بلغتها الأصلية دائماً — هذا هو العُرف في محدّدات اللغة
                    flag = if (lang == AppLanguage.ARABIC) "🇸🇦" else "🇬🇧",
                    label = if (lang == AppLanguage.ARABIC) "العربية" else "English",
                    selected = currentLanguage == lang,
                    onClick = { viewModel.setLanguage(lang); onDismiss() }
                )
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LanguageOption(
    flag: String,
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
        Text(text = flag, style = MaterialTheme.typography.titleLarge)
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
