package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.core.data.Countries

/**
 * عرض قراءة-فقط لاهتمامات المستخدم (بدون تحديد).
 * يُعرض داخل صفحة الملف الشخصي.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadOnlyInterestsChips(
    interestKeys: List<String>,
    modifier: Modifier = Modifier
) {
    if (interestKeys.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        interestKeys.forEach { key ->
            InterestPill(text = interestLabelAr(key))
        }
    }
}

@Composable
private fun InterestPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/** ترجمة مؤقتة لمفاتيح الاهتمامات — لاحقاً نستخدم القاموس من /api/interests. */
private fun interestLabelAr(key: String): String = key
