package com.chathala.hala.feature.settings.ui.privacy

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = viewModel(factory = PrivacyViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScaffold(
            title = S.get(R.string.privacy_title),
            onBack = onBack
        ) {
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                state.error != null -> ErrorState(
                    message = state.error ?: "",
                    onRetry = { viewModel.load() }
                )

                else -> {
                    val d = state.data ?: return@SettingsScaffold
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToggleRow(
                            title = S.get(R.string.privacy_show_distance),
                            subtitle = S.get(R.string.privacy_show_distance_desc),
                            checked = d.showDistance == true,
                            enabled = !state.updating,
                            onChange = viewModel::toggleDistance
                        )
                        ToggleRow(
                            title = S.get(R.string.privacy_stealth),
                            subtitle = S.get(R.string.privacy_stealth_desc),
                            checked = d.stealthMode == true,
                            enabled = !state.updating,
                            onChange = viewModel::toggleStealth
                        )
                        FriendRequestsSelector(
                            selected = d.friendRequests ?: "everyone",
                            enabled = !state.updating,
                            onSelect = viewModel::setFriendRequests
                        )
                        ReadOnlyRow(
                            title = S.get(R.string.privacy_profile_visibility),
                            value = when (d.profileVisibility) {
                                "public" -> S.get(R.string.privacy_visibility_public)
                                "private" -> S.get(R.string.privacy_visibility_private)
                                else -> d.profileVisibility ?: "-"
                            }
                        )
                        ReadOnlyRow(
                            title = S.get(R.string.privacy_last_seen),
                            value = if (d.showLastSeen == true)
                                S.get(R.string.enabled)
                            else
                                S.get(R.string.disabled)
                        )
                        ReadOnlyRow(
                            title = S.get(R.string.privacy_notification_sound),
                            value = if (d.notificationSound == true)
                                S.get(R.string.enabled)
                            else
                                S.get(R.string.disabled)
                        )
                    }
                }
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled
        )
    }
}

/** محدّد "من يستطيع إضافتي صديقاً" — 3 خيارات. */
@Composable
private fun FriendRequestsSelector(
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "everyone" to S.get(R.string.privacy_everyone),
        "contacts" to S.get(R.string.privacy_chatted_only),
        "nobody" to S.get(R.string.privacy_nobody)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = S.get(R.string.privacy_who_can_add),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                val isSel = selected == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .clickable(enabled = enabled) { onSelect(value) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
