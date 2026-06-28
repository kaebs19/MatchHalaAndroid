package com.chathala.hala.feature.settings.ui.discover

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
fun DiscoverSettingsScreen(
    onBack: () -> Unit,
    viewModel: DiscoverSettingsViewModel = viewModel(factory = DiscoverSettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var showPauseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }

    if (showPauseDialog) {
        PauseDurationDialog(
            onDismiss = { showPauseDialog = false },
            onPick = { hours ->
                showPauseDialog = false
                viewModel.setPauseDiscovery(enabled = true, durationHours = hours)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScaffold(
            title = stringResource(R.string.discover_settings_title),
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
                        // ── قسم: ظهور ملفي ──
                        SectionLabel(stringResource(R.string.discover_section_visibility))

                        DiscoverToggle(
                            icon = Icons.Filled.VisibilityOff,
                            title = stringResource(R.string.discover_hide_profile_title),
                            subtitle = stringResource(R.string.discover_hide_profile_desc),
                            checked = d.stealthMode == true,
                            enabled = enabled,
                            premium = true,
                            isPremium = state.isPremium,
                            // المفتاح = إخفاء → القيمة على الخادم = stealthMode (نفس الاتجاه)
                            onChange = { viewModel.toggle(DiscoverPref.STEALTH, it) }
                        )
                        DiscoverToggle(
                            icon = Icons.Filled.Cake,
                            title = stringResource(R.string.discover_hide_age_title),
                            subtitle = stringResource(R.string.discover_hide_age_desc),
                            checked = d.showAge == false,
                            enabled = enabled,
                            // المفتاح = إخفاء العمر → القيمة على الخادم = showAge (معكوسة)
                            onChange = { viewModel.toggle(DiscoverPref.SHOW_AGE, !it) }
                        )
                        DiscoverToggle(
                            icon = Icons.Filled.Public,
                            title = stringResource(R.string.discover_hide_country_title),
                            subtitle = stringResource(R.string.discover_hide_country_desc),
                            checked = d.showCountry == false,
                            enabled = enabled,
                            onChange = { viewModel.toggle(DiscoverPref.SHOW_COUNTRY, !it) }
                        )

                        val paused = d.discoveryPaused?.enabled == true
                        DiscoverToggle(
                            icon = Icons.Filled.PauseCircle,
                            title = stringResource(R.string.discover_pause_title),
                            subtitle = if (paused)
                                stringResource(R.string.discover_pause_active_desc)
                            else
                                stringResource(R.string.discover_pause_desc),
                            checked = paused,
                            enabled = enabled,
                            premium = true,
                            isPremium = state.isPremium,
                            onChange = { on ->
                                if (on) {
                                    // غير المشترك: نعرض رسالة الترقية بدل الحوار
                                    if (state.isPremium) showPauseDialog = true
                                    else viewModel.setPauseDiscovery(enabled = true)
                                } else {
                                    viewModel.setPauseDiscovery(enabled = false)
                                }
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        // ── قسم: طلبات المحادثة ──
                        SectionLabel(stringResource(R.string.discover_section_requests))

                        DiscoverToggle(
                            icon = Icons.Filled.MarkEmailRead,
                            title = stringResource(R.string.discover_stop_requests_title),
                            subtitle = stringResource(R.string.discover_stop_requests_desc),
                            checked = d.acceptingRequests == false,
                            enabled = enabled,
                            // المفتاح = إيقاف الدعوات → القيمة على الخادم = acceptingRequests (معكوسة)
                            onChange = { viewModel.toggle(DiscoverPref.ACCEPTING_REQUESTS, !it) }
                        )
                        DiscoverToggle(
                            icon = Icons.Filled.WorkspacePremium,
                            title = stringResource(R.string.discover_premium_only_title),
                            subtitle = stringResource(R.string.discover_premium_only_desc),
                            checked = d.premiumOnlyRequests == true,
                            enabled = enabled,
                            premium = true,
                            isPremium = state.isPremium,
                            onChange = { viewModel.toggle(DiscoverPref.PREMIUM_ONLY_REQUESTS, it) }
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

/** حوار اختيار مدة إيقاف الظهور في الاكتشاف. hours = null يعني حتى الاستئناف اليدوي. */
@Composable
private fun PauseDurationDialog(
    onDismiss: () -> Unit,
    onPick: (Int?) -> Unit
) {
    // (نص العنصر، المدة بالساعات أو null)
    val options = listOf(
        stringResource(R.string.discover_pause_24h) to 24,
        stringResource(R.string.discover_pause_3d) to 72,
        stringResource(R.string.discover_pause_1w) to 168,
        stringResource(R.string.discover_pause_until_resume) to null
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discover_pause_dialog_title)) },
        text = {
            Column {
                options.forEach { (label, hours) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onPick(hours) }
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun DiscoverToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    premium: Boolean = false,
    isPremium: Boolean = false,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (premium) {
                    Spacer(Modifier.size(6.dp))
                    PremiumBadge()
                }
            }
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
            // المفاتيح المدفوعة تبقى قابلة للنقر لغير المشتركين كي تظهر رسالة الترقية،
            // إلا أثناء التحديث
            enabled = enabled
        )
    }
}

@Composable
private fun PremiumBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.size(3.dp))
        Text(
            text = stringResource(R.string.discover_premium_badge),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
