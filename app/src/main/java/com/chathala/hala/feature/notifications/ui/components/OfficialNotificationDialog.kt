package com.chathala.hala.feature.notifications.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.feature.notifications.data.NotificationItem
import com.chathala.hala.feature.notifications.util.NotificationFormat
import com.chathala.hala.ui.components.HalaAsyncImage

/**
 * تفاصيل إشعار رسمي (من لوحة الإدارة): النص كاملاً، الصورة، وزر الرابط إن وُجد.
 *
 * الهوية هنا هوية التطبيق وحدها — لا اسم مشرف ولا صورته ولا انتقال لملفه.
 */
@Composable
fun OfficialNotificationDialog(item: NotificationItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val link = (item.data?.get("link") as? String)?.trim()?.takeIf { isSafeLink(it) }
    val image = item.image?.takeIf { it.isNotBlank() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                // هيدر متدرّج بشعار هلا الرسمي
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.dardasha_hala_log),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                }

                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = S.get(R.string.notif_official_account),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = NotificationFormat.timeAgo(item.createdAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item.title?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    item.body?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (image != null) {
                        Spacer(Modifier.height(14.dp))
                        HalaAsyncImage(
                            model = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    if (link != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                openLink(context, link)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = S.get(R.string.notif_open_link),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = S.get(R.string.action_close),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** روابط الويب وحدها — لا intent:// ولا مخططات تُشغّل شيئاً على الجهاز. */
fun isSafeLink(link: String?): Boolean =
    link != null && (link.startsWith("https://", ignoreCase = true) || link.startsWith("http://", ignoreCase = true))

fun openLink(context: Context, link: String) {
    if (!isSafeLink(link)) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
