package com.chathala.hala.feature.suspension.ui

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.PhonelinkErase
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.feature.suspension.data.DeviceBanData
import com.chathala.hala.feature.suspension.data.SuspensionMode
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.FormError
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.HalaTextField
import com.chathala.hala.ui.components.TextLink
import com.chathala.hala.ui.theme.HalaDimens
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SuspendedScreen(
    mode: SuspensionMode,
    onBackToLogin: () -> Unit,
    onLifted: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenContact: () -> Unit,
    viewModel: SuspendedViewModel = viewModel(factory = SuspendedViewModel.Factory(mode))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showExitConfirm by remember { mutableStateOf(false) }

    // فحص تلقائي عند عودة التطبيق للواجهة — يلتقط رفع التعليق دون تدخّل المستخدم
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.recheck(showFeedback = false)
    }

    // عند رفع التعليق → رسالة ترحيب احترافية ثم الدخول للتطبيق
    if (state.lifted) {
        WelcomeBackDialog(onEnter = onLifted)
    }

    // تأكيد قبل العودة لتسجيل الدخول
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(S.get(R.string.logout_confirm_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    S.get(R.string.logout_confirm_body) +
                        S.get(R.string.logout_confirm_question)
                )
            },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onBackToLogin() }) {
                    Text(S.get(R.string.logout_yes), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text(S.get(R.string.logout_stay)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HalaDimens.Spacing.xl, vertical = HalaDimens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── رأس الشاشة ───────────────────────────────────────────
        val headerIcon = if (mode == SuspensionMode.DEVICE) Icons.Outlined.PhonelinkErase else Icons.Outlined.Block
        val title = when {
            mode == SuspensionMode.DEVICE -> S.get(R.string.device_banned_title)
            state.account?.isPermanent == true -> S.get(R.string.account_banned_permanently)
            else -> S.get(R.string.account_suspended_temporarily)
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = headerIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(HalaDimens.Icon.xxl)
            )
        }

        Spacer(Modifier.height(HalaDimens.Spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(HalaDimens.Spacing.sm))
        Text(
            text = S.get(R.string.suspension_policy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // ── إعادة التحقق من رفع التعليق ──────────────────────────
        // فقط للتعليق أثناء الجلسة (token == null يعني وجود جلسة مخزّنة)؛
        // تعليق وقت تسجيل الدخول حلّه إعادة الدخول.
        if (mode == SuspensionMode.ACCOUNT && state.account?.token == null) {
            Spacer(Modifier.height(HalaDimens.Spacing.lg))
            OutlinedButton(
                onClick = { viewModel.recheck(showFeedback = true) },
                enabled = !state.rechecking
            ) {
                if (state.rechecking) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(HalaDimens.Spacing.sm))
                }
                Text(S.get(R.string.check_suspension_lifted))
            }
            FormError(state.recheckMessage)
        }

        Spacer(Modifier.height(HalaDimens.Spacing.xl))

        // ── بطاقة التفاصيل ───────────────────────────────────────
        when (mode) {
            SuspensionMode.ACCOUNT -> AccountDetailsCard(state)
            SuspensionMode.DEVICE -> when {
                state.loadingDetails -> Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                state.detailsError != null -> ErrorState(
                    message = state.detailsError ?: S.get(R.string.err_load_details_failed),
                    onRetry = { viewModel.loadDeviceBan() }
                )
                state.device != null -> DeviceDetailsCard(state.device!!)
            }
        }

        Spacer(Modifier.height(HalaDimens.Spacing.xl))

        // ── قسم الاستئناف ────────────────────────────────────────
        if (state.appealSent) {
            AppealSentCard(state.appealSentMessage)
        } else if (state.canAppeal && !(mode == SuspensionMode.DEVICE && state.loadingDetails)) {
            AppealForm(
                mode = mode,
                submitting = state.submitting,
                error = state.appealError,
                onSubmit = viewModel::submitAppeal,
                onClearError = viewModel::clearAppealFeedback
            )
        }

        // ── الاستئنافات السابقة (وضع الحساب) ──────────────────────
        if (mode == SuspensionMode.ACCOUNT && state.previousAppeals.isNotEmpty()) {
            Spacer(Modifier.height(HalaDimens.Spacing.xl))
            PreviousAppealsCard(state.previousAppeals)
        }

        Spacer(Modifier.height(HalaDimens.Spacing.xxl))

        // ── روابط سريعة (موحّدة مع شاشة طلباتي) ───────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextLink(text = S.get(R.string.legal_terms_of_use), onClick = onOpenTerms)
            Text("  •  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextLink(text = S.get(R.string.legal_privacy_policy), onClick = onOpenPrivacy)
            Text("  •  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextLink(text = S.get(R.string.premium_contact_us), onClick = onOpenContact)
            Text("  •  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextLink(
                text = S.get(R.string.social_instagram_short),
                onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(
                                    com.chathala.hala.core.config.OfficialContacts.INSTAGRAM
                                )
                            )
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(HalaDimens.Spacing.lg))
        // زر ثانوي غير بارز — تسجيل الخروج
        TextButton(onClick = { showExitConfirm = true }) {
            Text(
                text = S.get(R.string.action_sign_out),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviousAppealsCard(appeals: List<com.chathala.hala.feature.settings.data.AppealItem>) {
    DetailCard(icon = Icons.Filled.Gavel, heading = S.get(R.string.my_previous_requests)) {
        appeals.forEachIndexed { index, a ->
            if (index > 0) Spacer(Modifier.height(HalaDimens.Spacing.sm))
            val (statusLabel, statusColor) = com.chathala.hala.feature.settings.ui.account
                .appealStatusLabel(a.status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = a.reason?.takeIf { it.isNotBlank() } ?: S.get(R.string.action_appeal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    a.createdAt?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = formatDate(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.size(HalaDimens.Spacing.sm))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountDetailsCard(state: SuspendedUiState) {
    val info = state.account
    DetailCard(icon = Icons.Filled.Gavel, heading = S.get(R.string.action_details_title)) {
        info?.name?.takeIf { it.isNotBlank() }?.let { DetailRow(S.get(R.string.label_name), it) }
        info?.email?.takeIf { it.isNotBlank() }?.let { DetailRow(S.get(R.string.label_email), it) }
        info?.userId?.takeIf { it.isNotBlank() }?.let { DetailRow(S.get(R.string.label_id), it) }
        DetailRow(S.get(R.string.label_action_type), accountActionLabel(info?.isPermanent == true, info?.level ?: 0))
        DetailRow(S.get(R.string.label_reason), info?.reason?.takeIf { it.isNotBlank() } ?: S.get(R.string.violation_policy_breach))
        if (info?.isPermanent == false && !info.suspendedUntil.isNullOrBlank()) {
            DetailRow(S.get(R.string.label_expires_on), formatDate(info.suspendedUntil))
        }
        if ((info?.level ?: 0) > 0) DetailRow(S.get(R.string.label_violation_severity), "${info?.level} / 5")
    }
}

/** اسم الإجراء حسب النوع: حظر دائم / إيقاف مؤقت / تقييد. */
private fun accountActionLabel(isPermanent: Boolean, level: Int): String = when {
    isPermanent -> S.get(R.string.action_permanent_ban)
    level <= 1 -> S.get(R.string.action_temporary_restriction)
    else -> S.get(R.string.action_temporary_suspension)
}

@Composable
private fun DeviceDetailsCard(device: DeviceBanData) {
    val acc = device.originalAccount
    Column {
        // بطاقة الحساب السابق المرتبط بالجهاز
        DetailCard(icon = Icons.Filled.Person, heading = S.get(R.string.device_previous_account)) {
            if (acc?.maskedName.isNullOrBlank() && acc?.maskedEmail.isNullOrBlank()) {
                Text(
                    text = S.get(R.string.device_previous_account_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                acc?.maskedName?.takeIf { it.isNotBlank() }?.let { DetailRow(S.get(R.string.label_name), it) }
                acc?.maskedEmail?.takeIf { it.isNotBlank() }?.let { DetailRow(S.get(R.string.label_email), it) }
                acc?.halaId?.takeIf { it.isNotBlank() }?.let { DetailRow(S.get(R.string.label_id), it) }
                acc?.accountCreatedAt?.let { DetailRow(S.get(R.string.label_account_created), formatDate(it)) }
            }
        }

        Spacer(Modifier.height(HalaDimens.Spacing.lg))

        // بطاقة تفاصيل الحظر
        DetailCard(icon = Icons.Outlined.Block, heading = S.get(R.string.ban_details_title)) {
            DetailRow(S.get(R.string.label_ban_reason), device.reason?.takeIf { it.isNotBlank() } ?: S.get(R.string.violation_policy_breach))
            device.bannedAt?.let { DetailRow(S.get(R.string.label_ban_date), formatDate(it)) }
            device.bannedBy?.let { DetailRow(S.get(R.string.label_ban_source), bannedByLabel(it)) }
        }

        Spacer(Modifier.height(HalaDimens.Spacing.lg))

        // تنبيه مهم
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            tonalElevation = 0.dp
        ) {
            Row(modifier = Modifier.padding(HalaDimens.Spacing.lg)) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(HalaDimens.Icon.md)
                )
                Spacer(Modifier.size(HalaDimens.Spacing.sm))
                Text(
                    text = S.get(R.string.device_no_new_account) +
                        S.get(R.string.device_appeal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** يحوّل قيمة bannedBy إلى نص عربي واضح. */
private fun bannedByLabel(value: String): String = when (value) {
    "admin" -> S.get(R.string.ban_source_admin)
    "auto" -> S.get(R.string.ban_source_system)
    "spam_system" -> S.get(R.string.ban_source_antispam)
    else -> value
}

@Composable
private fun DetailCard(
    icon: ImageVector,
    heading: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(HalaDimens.Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(HalaDimens.Icon.md)
                )
                Spacer(Modifier.size(HalaDimens.Spacing.sm))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(HalaDimens.Spacing.md))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HalaDimens.Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(HalaDimens.Spacing.md))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun AppealForm(
    mode: SuspensionMode,
    submitting: Boolean,
    error: String?,
    onSubmit: (reason: String, email: String?) -> Unit,
    onClearError: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    DetailCard(icon = Icons.Filled.Gavel, heading = S.get(R.string.submit_appeal_title)) {
        Text(
            text = S.get(R.string.submit_appeal_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(HalaDimens.Spacing.md))

        if (mode == SuspensionMode.DEVICE) {
            HalaTextField(
                value = email,
                onValueChange = { email = it; onClearError() },
                label = S.get(R.string.appeal_email_optional),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            )
            Spacer(Modifier.height(HalaDimens.Spacing.md))
        }

        HalaTextField(
            value = reason,
            onValueChange = { reason = it; onClearError() },
            label = S.get(R.string.appeal_reason_label),
            singleLine = false
        )

        FormError(error)

        Spacer(Modifier.height(HalaDimens.Spacing.md))
        HalaPrimaryButton(
            text = S.get(R.string.appeal_submit),
            loading = submitting,
            enabled = reason.isNotBlank(),
            onClick = { onSubmit(reason, email.ifBlank { null }) }
        )
    }
}

@Composable
private fun AppealSentCard(message: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HalaDimens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HalaDimens.Icon.xl)
            )
            Spacer(Modifier.height(HalaDimens.Spacing.md))
            Text(
                text = message ?: S.get(R.string.appeal_sent_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** رسالة ترحيب احترافية بعد رفع التعليق + تذكير بالالتزام بالسياسة. */
@Composable
private fun WelcomeBackDialog(onEnter: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* قرار إلزامي — الدخول فقط */ },
        icon = {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HalaDimens.Icon.xl)
            )
        },
        title = {
            Text(
                text = S.get(R.string.suspension_lifted),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = S.get(R.string.welcome_back_hala) +
                    S.get(R.string.welcome_back_body) +
                    S.get(R.string.welcome_back_body_tail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onEnter) {
                Text(S.get(R.string.enter_the_app), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/** يحوّل تاريخ ISO إلى صيغة عربية مختصرة؛ يرجّع القيمة كما هي إن تعذّر التحليل. */
private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return S.get(R.string.label_unspecified)
    return try {
        val dt = OffsetDateTime.parse(iso)
        dt.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ar")))
    } catch (_: Exception) {
        iso
    }
}
