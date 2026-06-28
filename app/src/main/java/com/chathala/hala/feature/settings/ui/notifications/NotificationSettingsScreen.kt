package com.chathala.hala.feature.settings.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = viewModel(factory = NotificationSettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScaffold(
            title = stringResource(R.string.notif_settings_title),
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
                    val enabled = !state.updating
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // المفتاح الرئيسي
                        NotifToggle(
                            icon = Icons.Filled.NotificationsActive,
                            title = stringResource(R.string.notif_master_title),
                            subtitle = stringResource(R.string.notif_master_desc),
                            checked = d.pushEnabled,
                            enabled = enabled,
                            highlighted = true,
                            onChange = { viewModel.toggle(NotifPrefKey.PUSH_ENABLED, it) }
                        )

                        Text(
                            text = stringResource(R.string.notif_categories_header),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        // الفئات — تختفي عند إيقاف المفتاح الرئيسي
                        AnimatedVisibility(
                            visible = d.pushEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                NotifToggle(
                                    icon = Icons.Filled.Chat,
                                    title = stringResource(R.string.notif_invitations_title),
                                    subtitle = stringResource(R.string.notif_invitations_desc),
                                    checked = d.invitations,
                                    enabled = enabled,
                                    onChange = { viewModel.toggle(NotifPrefKey.INVITATIONS, it) }
                                )
                                NotifToggle(
                                    icon = Icons.Filled.MarkChatUnread,
                                    title = stringResource(R.string.notif_messages_title),
                                    subtitle = stringResource(R.string.notif_messages_desc),
                                    checked = d.messages,
                                    enabled = enabled,
                                    onChange = { viewModel.toggle(NotifPrefKey.MESSAGES, it) }
                                )
                                NotifToggle(
                                    icon = Icons.Filled.Visibility,
                                    title = stringResource(R.string.notif_profile_visits_title),
                                    subtitle = stringResource(R.string.notif_profile_visits_desc),
                                    checked = d.profileVisits,
                                    enabled = enabled,
                                    onChange = { viewModel.toggle(NotifPrefKey.PROFILE_VISITS, it) }
                                )
                                NotifToggle(
                                    icon = Icons.Filled.Campaign,
                                    title = stringResource(R.string.notif_app_alerts_title),
                                    subtitle = stringResource(R.string.notif_app_alerts_desc),
                                    checked = d.appAlerts,
                                    enabled = enabled,
                                    onChange = { viewModel.toggle(NotifPrefKey.APP_ALERTS, it) }
                                )
                            }
                        }

                        // ── ساعات الهدوء ──
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.notif_quiet_header),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                        val dnd = state.dnd
                        NotifToggle(
                            icon = Icons.Filled.Bedtime,
                            title = stringResource(R.string.notif_quiet_title),
                            subtitle = stringResource(R.string.notif_quiet_desc),
                            checked = dnd?.enabled == true,
                            enabled = enabled && dnd != null,
                            onChange = { viewModel.toggleQuietHours(it) }
                        )
                        AnimatedVisibility(
                            visible = dnd?.enabled == true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TimeRow(
                                    label = stringResource(R.string.notif_quiet_from),
                                    hour = dnd?.startHour ?: 23,
                                    minute = dnd?.startMinute ?: 0,
                                    enabled = enabled,
                                    onPick = { h, m -> viewModel.setQuietTime(isStart = true, hour = h, minute = m) }
                                )
                                TimeRow(
                                    label = stringResource(R.string.notif_quiet_to),
                                    hour = dnd?.endHour ?: 7,
                                    minute = dnd?.endMinute ?: 0,
                                    enabled = enabled,
                                    onPick = { h, m -> viewModel.setQuietTime(isStart = false, hour = h, minute = m) }
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.notif_critical_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
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
private fun TimeRow(
    label: String,
    hour: Int,
    minute: Int,
    enabled: Boolean,
    onPick: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val timeText = "%02d:%02d".format(hour, minute)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled) {
                TimePickerDialog(
                    context,
                    { _, h, m -> onPick(h, m) },
                    hour, minute, true
                ).show()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NotifToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    highlighted: Boolean = false,
    onChange: (Boolean) -> Unit
) {
    val bg = if (highlighted)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(14.dp))
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
