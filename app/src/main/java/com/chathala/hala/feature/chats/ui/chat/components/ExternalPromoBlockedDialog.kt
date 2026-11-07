package com.chathala.hala.feature.chats.ui.chat.components

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chathala.hala.core.network.RestrictionCenter
import com.chathala.hala.feature.chats.data.ExternalPromoBlockedInfo

private val PromoYellow = Color(0xFFF2B705)
private val PromoOrange = Color(0xFFFF8C00)
private val PromoRed = Color(0xFFE53935)

/**
 * ورقة تنبيه الحسابات الخارجية (مقابل `ExternalPromoSheet` في iOS). كل النصوص من الخادم:
 * - المحاولات 1–4: العنوان والنص يتغيّران مع المحاولة، أصفر في 1–2 وبرتقالي في 3–4.
 * - «المحاولة X من 5» و«عند التكرار» يختفيان في `locked`/`paused`.
 * - `serverMessage` يُعرض في `locked`/`paused` فقط.
 * - زرّ «فهمت» مثبّت خارج التمرير، ولا يُغلق بالضغط خارجها.
 */
@Composable
fun ExternalPromoBlockedDialog(
    info: ExternalPromoBlockedInfo,
    onDismiss: () -> Unit,
    onAppeal: ((reason: String) -> Unit)? = null
) {
    var showAppealDialog by remember { mutableStateOf(false) }
    if (showAppealDialog) {
        AppealDialog(
            onDismiss = { showAppealDialog = false },
            onSubmit = { reason ->
                showAppealDialog = false
                onAppeal?.invoke(reason)
            }
        )
    }

    val severity = info.severity
    val locked = info.lockApplied == true || info.suspended == true || severity == "locked" || severity == "suspended"
    val paused = severity == "paused"
    val violations = info.violations ?: 0
    val threshold = info.threshold ?: 5

    val accent = when {
        severity == "suspended" || locked -> PromoRed
        paused || severity == "last_warning" -> PromoOrange
        violations >= 3 -> PromoOrange
        severity == "repeated" && violations == 0 -> PromoOrange
        else -> PromoYellow
    }
    val icon: ImageVector = when {
        locked -> Icons.Filled.Error
        paused -> Icons.Filled.PauseCircle
        severity == "last_warning" || violations >= 3 -> Icons.Filled.Warning
        else -> Icons.Filled.PanTool
    }
    val topMessage = S.serverOr(info.message, R.string.promo_default_message)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.85f
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = maxH)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // المحتوى القابل للتمرير — weight(fill=false) يُبقي الزرّ ظاهراً دائماً
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(14.dp))
                Text(
                    text = info.title ?: S.get(R.string.msg_blocked_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = topMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // عدّاد «المحاولة X من 5» — يختفي في الإيقاف
                if (!locked && !paused && violations > 0 && threshold > 0) {
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            S.get(R.string.promo_attempt),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            S.get(R.string.promo_attempt_of, violations, threshold),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (violations.toFloat() / threshold).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.2f)
                    )
                }

                // بطاقة الإيقاف: المرة + المدة بالدقائق
                val minutes = info.durationMinutes?.takeIf { it > 0 }
                    ?: info.durationHours?.takeIf { it > 0 }?.times(60)
                val lockCount = info.lockCount ?: 0
                if (locked && lockCount > 0 && minutes != null) {
                    Spacer(Modifier.height(16.dp))
                    LockInfoCard(accent, lockCount, RestrictionCenter.formatDuration(minutes))
                }

                // رسالة السيرفر — للإيقاف فقط، وإن لم تكرّر النص العلوي
                val serverMsg = info.serverMessage?.takeIf { it.isNotBlank() }
                if ((locked || paused) && serverMsg != null && !isRedundant(serverMsg, topMessage)) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = serverMsg,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                if (!locked && !paused) {
                    PromoInfoRow(Icons.Filled.PauseCircle, accent, S.get(R.string.promo_repeat_title), S.get(R.string.promo_repeat_desc))
                    Spacer(Modifier.height(8.dp))
                }
                PromoInfoRow(Icons.Filled.Shield, accent, S.get(R.string.promo_review_title), S.get(R.string.promo_review_desc))

                if (onAppeal != null && !locked && !paused) {
                    TextButton(onClick = { showAppealDialog = true }) {
                        Text(S.get(R.string.promo_appeal_title), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // زرّ «فهمت» مثبّت أسفل الورقة خارج التمرير
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        S.get(R.string.promo_got_it),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/** هل رسالة الخادم تكرار للنص العلوي؟ (مقارنة أول 20 حرفاً بعد إزالة المسافات والترقيم) */
private fun isRedundant(server: String, top: String): Boolean {
    fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
    val a = norm(server); val b = norm(top)
    return if (a.length < 20 || b.length < 20) a == b else a.take(20) == b.take(20)
}

@Composable
private fun LockInfoCard(accent: Color, lockCount: Int, duration: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                S.get(R.string.promo_pause_card_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatItem(Modifier.weight(1f), accent, Icons.Filled.Tag, S.get(R.string.promo_pause_count), "$lockCount")
            StatItem(Modifier.weight(1f), accent, Icons.Filled.Schedule, S.get(R.string.promo_pause_duration), duration)
        }
    }
}

@Composable
private fun StatItem(modifier: Modifier, accent: Color, icon: ImageVector, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppealDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(S.get(R.string.promo_appeal_title)) },
        text = {
            Column {
                Text(
                    text = S.get(R.string.promo_appeal_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text(S.get(R.string.promo_appeal_hint)) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(reason.trim()) }, enabled = reason.isNotBlank()) {
                Text(S.get(R.string.action_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(S.get(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun PromoInfoRow(icon: ImageVector, tint: Color, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
