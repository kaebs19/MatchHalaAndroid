package com.chathala.hala.feature.push

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.ui.components.HalaAsyncImage
import kotlinx.coroutines.delay

/** كم يبقى الشريط ظاهراً قبل أن ينزوي وحده. */
private const val AUTO_DISMISS_MS = 4_500L

/** مسافة السحب لأعلى التي تُعدّ رفضاً للشريط. */
private const val SWIPE_DISMISS_PX = -24f

/**
 * شريط الرسالة الواردة داخل التطبيق.
 *
 * يُركَّب مرّة واحدة فوق كامل الواجهة في [com.chathala.hala.MainActivity]، فيعمل
 * أياً كانت الشاشة المعروضة. لا يظهر لرسائل المحادثة المفتوحة ولا لرسائل المستخدم
 * نفسه — انظر [InAppAlerts].
 */
@Composable
fun InAppMessageBanner(
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alert by InAppAlerts.current.collectAsStateWithLifecycle()

    // المؤقّت مرتبط بمعرّف الرسالة: وصول رسالة أخرى يستبدل الشريط ويُعيد العدّ
    // من الصفر بدل أن يرث ما تبقّى من عمر سابقتها.
    LaunchedEffect(alert?.id) {
        val id = alert?.id ?: return@LaunchedEffect
        delay(AUTO_DISMISS_MS)
        InAppAlerts.dismiss(id)
    }

    // الحالة تصير `null` لحظة الرفض، بينما حركة الخروج لم تبدأ بعد — فلو قرأ
    // المحتوى الحالةَ مباشرةً لاختفى نصّه فجأةً ثم انزلق إطارٌ فارغ. نحتفظ بآخر
    // قيمة غير فارغة ليخرج الشريط بمحتواه.
    var lastShown by remember { mutableStateOf<InAppMessageAlert?>(null) }
    LaunchedEffect(alert) { if (alert != null) lastShown = alert }

    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val shown = lastShown ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .pointerInput(shown.id) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < SWIPE_DISMISS_PX) InAppAlerts.dismiss(shown.id)
                    }
                }
                .clickable {
                    InAppAlerts.dismiss(shown.id)
                    onOpenConversation(shown.conversationId)
                },
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HalaAsyncImage(
                    model = shown.senderImage,
                    contentDescription = null,
                    fallbackName = shown.senderName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shown.senderName ?: S.get(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = previewOf(shown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** نفس تسميات معاينة آخر رسالة في قائمة المحادثات، حتى لا يختلف وصف النوع بين الشاشتين. */
@Composable
private fun previewOf(alert: InAppMessageAlert): String = when (alert.type) {
    "image" -> S.get(R.string.msg_type_photo)
    "audio" -> S.get(R.string.msg_type_voice)
    "video" -> S.get(R.string.msg_type_video)
    "file" -> S.get(R.string.msg_type_file)
    "system" -> S.get(R.string.chat_update_placeholder)
    else -> alert.content?.takeIf { it.isNotBlank() } ?: S.get(R.string.chat_start)
}
