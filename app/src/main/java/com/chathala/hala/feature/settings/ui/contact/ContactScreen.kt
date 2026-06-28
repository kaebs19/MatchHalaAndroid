package com.chathala.hala.feature.settings.ui.contact

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.chathala.hala.R
import com.chathala.hala.core.config.OfficialContacts
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold

@Composable
fun ContactScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun open(intent: Intent) {
        runCatching { context.startActivity(intent) }
    }

    SettingsScaffold(
        title = stringResource(R.string.contact_title),
        onBack = onBack
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // مقدمة
            Text(
                text = "نحن هنا لمساعدتك",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "تواصل مع فريق هلا عبر القنوات التالية، وسنردّ عليك في أقرب وقت.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(4.dp))

            ContactRow(
                icon = Icons.Filled.Email,
                iconColor = Color(0xFFE85A9B),
                label = stringResource(R.string.contact_email),
                value = OfficialContacts.EMAIL,
                onClick = {
                    open(Intent(Intent.ACTION_SENDTO, "mailto:${OfficialContacts.EMAIL}".toUri()))
                }
            )

            ContactRow(
                icon = Icons.Filled.Language,
                iconColor = Color(0xFF2196F3),
                label = stringResource(R.string.contact_website),
                value = "www.chathala.com",
                onClick = {
                    open(Intent(Intent.ACTION_VIEW, OfficialContacts.WEBSITE.toUri()))
                }
            )

            ContactRow(
                icon = Icons.Filled.CameraAlt,
                iconColor = Color(0xFFC13584),
                label = "إنستغرام",
                value = "@hala.chat",
                onClick = {
                    open(Intent(Intent.ACTION_VIEW, OfficialContacts.INSTAGRAM.toUri()))
                }
            )
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor)
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
