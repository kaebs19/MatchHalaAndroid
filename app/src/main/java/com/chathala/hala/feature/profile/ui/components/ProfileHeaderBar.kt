package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * شريط أعلى صفحة الملف الشخصي — يُرسم فوق تدرّج الهيدر، لذا نصّه وأيقوناته بيضاء
 * داخل حاويات شفّافة. المجموعتان كما في iOS: (تعديل، مظهر) و(QR، إعدادات).
 * لا زر خروج هنا — موضعه الإعدادات فقط.
 */
@Composable
fun ProfileHeaderBar(
    title: String,
    onSettings: () -> Unit,
    onShowQr: () -> Unit,
    onTheme: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ActionPill(
            actions = listOf(
                Icons.Filled.Edit to onEdit,
                Icons.Filled.Palette to onTheme
            )
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        ActionPill(
            actions = listOf(
                Icons.Filled.QrCode to onShowQr,
                Icons.Filled.Settings to onSettings
            )
        )
    }
}

@Composable
private fun ActionPill(
    actions: List<Pair<ImageVector, () -> Unit>>
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { (icon, onClick) ->
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
