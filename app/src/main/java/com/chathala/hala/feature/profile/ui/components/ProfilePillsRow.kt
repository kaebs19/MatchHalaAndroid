package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * صفّ Pills تحت الاسم: الدولة + الجنس (أو أيّها متاح).
 */
@Composable
fun ProfilePillsRow(
    countryLabel: String?,
    gender: String?,
    modifier: Modifier = Modifier
) {
    if (countryLabel.isNullOrBlank() && gender.isNullOrBlank()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!countryLabel.isNullOrBlank()) {
            Pill(
                text = countryLabel,
                accent = ProfilePalette.Country
            )
            if (!gender.isNullOrBlank()) Spacer(Modifier.size(10.dp))
        }
        if (!gender.isNullOrBlank()) {
            val isMale = gender == "male"
            Pill(
                text = if (isMale) "ذكر" else "أنثى",
                icon = if (isMale) Icons.Filled.Male else Icons.Filled.Female,
                accent = if (isMale) ProfilePalette.Gender else Color(0xFFFF4D8F)
            )
        }
    }
}

@Composable
private fun Pill(
    text: String,
    accent: Color,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
        }
        Text(
            text = text,
            color = accent,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
