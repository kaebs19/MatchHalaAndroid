package com.chathala.hala.feature.auth.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NoAdultContent
import androidx.compose.material.icons.filled.ReportGmailerrorred
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.R
import com.chathala.hala.ui.components.HalaPrimaryButton

private data class PolicyRule(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val severe: Boolean = false
)

@Composable
fun ContentPolicyScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }

    val rules = listOf(
        PolicyRule(
            Icons.Filled.NoAdultContent,
            "المحتوى الجنسي والإباحي",
            "يُمنع نشر أو إرسال أي صور أو فيديو أو نصوص جنسية أو إباحية."
        ),
        PolicyRule(
            Icons.Filled.ChildCare,
            "حماية الأطفال",
            "أي محتوى يتعلّق بإساءة معاملة القُصّر أو استغلالهم يؤدي إلى حظر دائم وإبلاغ الجهات المختصّة.",
            severe = true
        ),
        PolicyRule(
            Icons.Filled.Block,
            "الأسماء والصور المسيئة",
            "اسم المستخدم أو صورة الملف ذات الطابع الجنسي أو الإباحي تُعرّض الحساب للحظر."
        ),
        PolicyRule(
            Icons.Outlined.Mosque,
            "الإساءة الدينية والطائفية",
            "يُمنع ازدراء الأديان أو إثارة النعرات الطائفية والإساءة للمعتقدات."
        ),
        PolicyRule(
            Icons.Filled.Gavel,
            "المحتوى السياسي المثير للفتنة",
            "يُمنع الترويج للعنف أو الكراهية أو إثارة الفتن لأغراض سياسية."
        ),
        PolicyRule(
            Icons.Filled.ReportGmailerrorred,
            "العنف والكراهية والتنمّر",
            "يُمنع التهديد والتحرّش وخطاب الكراهية والتنمّر بكل أشكاله."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // أيقونة الحماية
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.policy_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.policy_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        rules.forEach { rule ->
            RuleCard(rule)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.policy_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(20.dp))

        // موافقة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { checked = !checked }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.policy_check),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(20.dp))

        HalaPrimaryButton(
            text = stringResource(R.string.policy_agree),
            enabled = checked,
            onClick = onAgree
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { showDeclineDialog = true }) {
                Text(
                    text = stringResource(R.string.policy_decline),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.policy_decline_title)) },
            text = { Text(stringResource(R.string.policy_decline_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeclineDialog = false
                    onDecline()
                }) {
                    Text(
                        stringResource(R.string.policy_decline_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text(stringResource(R.string.policy_back))
                }
            }
        )
    }
}

@Composable
private fun RuleCard(rule: PolicyRule) {
    val accent = if (rule.severe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = if (rule.severe) 0.10f else 0.06f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = rule.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = rule.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = rule.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
