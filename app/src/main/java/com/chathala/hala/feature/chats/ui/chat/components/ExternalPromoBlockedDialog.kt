package com.chathala.hala.feature.chats.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chathala.hala.feature.chats.data.ExternalPromoBlockedInfo

private val OrangeColor = Color(0xFFFF8C00)
private val DarkBg = Color(0xFF1A1200)

@OptIn(ExperimentalLayoutApi::class)
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // العنوان
            Text(
                text = info.title ?: "تم حجب رسالتك",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // الوصف
            Text(
                text = info.message ?: "تم التعرف تلقائياً على مشاركة حساب خارجي. سياسة المنصة تمنع ذلك، وتكرار مشاركة حسابات أو أرقام يقيّد حسابك آلياً.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // شريط التحذير مع العداد
            val violations = info.violations ?: 1
            val threshold = info.threshold ?: 5

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التحذير",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$violations / $threshold",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OrangeColor
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (violations.toFloat() / threshold.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = OrangeColor,
                trackColor = OrangeColor.copy(alpha = 0.2f)
            )

            Spacer(Modifier.height(16.dp))

            // رسالة السيرفر التفصيلية (إذا وُجدت)
            val detailMsg = info.serverMessage ?: "تم التعرف تلقائياً على مشاركة حساب خارجي. سياسة المنصة تمنع نشر أو طلب الحسابات والأرقام، وتكرار ذلك يقيّد حسابك تلقائياً — رسائلك أمانة، حافظ على التواصل داخل التطبيق."
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrangeColor.copy(alpha = 0.12f))
                    .padding(14.dp)
            ) {
                Text(
                    text = detailMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = OrangeColor,
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(16.dp))

            // ميزات/سياسات
            PromoInfoRow(
                icon = Icons.Filled.AutoFixHigh,
                iconTint = Color(0xFF9E9E9E),
                title = "كشف تلقائي",
                description = "النظام يكشف الحسابات الخارجية تلقائياً. يُرجى الالتزام بسياسة التطبيق."
            )
            Spacer(Modifier.height(10.dp))
            PromoInfoRow(
                icon = Icons.Filled.Shield,
                iconTint = Color(0xFF6A1B9A),
                title = "رسائلك أمانة",
                description = "نحرص على بقاء التواصل داخل المنصة لحمايتك وحماية الطرف الآخر."
            )

            // الكلمات المكتشفة
            if (!info.detectedPatterns.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrangeColor.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "الكلمات المكتشفة:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OrangeColor
                        )
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            info.detectedPatterns.forEach { word ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(OrangeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = word,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OrangeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // زر الإغلاق
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeColor)
            ) {
                Text(
                    text = "فهمت والتزم",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            if (onAppeal != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAppealDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "استئناف الحجب",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrangeColor
                    )
                }
            }
        }
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
        title = { Text("استئناف الحجب") },
        text = {
            Column {
                Text(
                    text = "اكتب سبب استئنافك. سيتم مراجعته في أقرب فرصة ممكنة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("سبب الاستئناف...") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason.trim()) },
                enabled = reason.isNotBlank()
            ) { Text("إرسال") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun PromoInfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(12.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
