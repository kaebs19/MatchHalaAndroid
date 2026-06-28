package com.chathala.hala.feature.chats.ui.chat.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chathala.hala.R

/** مصدر الصورة المُختارة للمعاينة. */
enum class ImageSource { GALLERY, CAMERA }

/** خيارات مدة التدمير الذاتي (ثوانٍ)، إضافةً للوضع العادي. */
private val DURATION_OPTIONS = listOf(5, 10, 20, 30)

/**
 * صفحة معاينة احترافية بملء الشاشة قبل إرسال الصورة:
 *  - تعرض الصورة كاملةً مع شارة المصدر (معرض/كاميرا).
 *  - تتيح اختيار مؤقّت تدمير ذاتي (5/10/20/30 ث) أو إرسال عادي.
 */
@Composable
fun ImagePreviewScreen(
    previewUri: Uri,
    source: ImageSource,
    onSend: (durationSeconds: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    // null = إرسال عادي بدون اختفاء
    var selectedDuration by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ── الصورة ──
            AsyncImage(
                model = previewUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // ── الشريط العلوي: إغلاق + شارة المصدر ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.preview_close),
                    onClick = onDismiss
                )
                SourceBadge(source = source)
            }

            // ── اللوحة السفلية: المؤقّت + زر الإرسال ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.preview_self_destruct),
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }

                Spacer(Modifier.size(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // إرسال عادي (بدون اختفاء)
                    DurationChip(
                        label = stringResource(R.string.preview_duration_normal),
                        selected = selectedDuration == null,
                        modifier = Modifier.weight(1f)
                    ) { selectedDuration = null }
                    DURATION_OPTIONS.forEach { sec ->
                        DurationChip(
                            label = "${sec}ث",
                            selected = selectedDuration == sec,
                            modifier = Modifier.weight(1f)
                        ) { selectedDuration = sec }
                    }
                }

                Spacer(Modifier.size(16.dp))

                // زر الإرسال
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        .clickable { onSend(selectedDuration) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDuration == null)
                            stringResource(R.string.preview_send)
                        else
                            stringResource(R.string.preview_send_disappearing, selectedDuration!!),
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall
                            .copy(fontWeight = FontWeight.Bold),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(source: ImageSource) {
    val (icon, label) = when (source) {
        ImageSource.CAMERA -> Icons.Filled.PhotoCamera to stringResource(R.string.preview_source_camera)
        ImageSource.GALLERY -> Icons.Filled.PhotoLibrary to stringResource(R.string.preview_source_gallery)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = androidx.compose.material3.MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) primary else Color.White.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = if (selected) primary else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                .copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else Color.White
        )
    }
}
