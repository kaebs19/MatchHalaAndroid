package com.chathala.hala.feature.stories.ui

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.showToast
import com.chathala.hala.feature.notifications.util.NotificationFormat
import com.chathala.hala.feature.stories.data.Story
import com.chathala.hala.feature.stories.data.StoryViewersData
import com.chathala.hala.ui.components.HalaAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * ورقة مشاهدي قصتي: العدد دائماً، والأسماء لمشترك هلا+ فقط (📸 بجانب من التقط لقطة).
 * غير المشترك: «N شاهدوا قصتك» + دعوة الاشتراك.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewersSheet(
    story: Story,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onOpenPremium: () -> Unit
) {
    val manager = (LocalContext.current.applicationContext as HalaApp).storyManager
    var data by remember { mutableStateOf<StoryViewersData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(story.id, story.viewCount, story.screenshotCount, story.reactionCount) {
        when (val r = manager.viewers(story.id)) {
            is NetworkResult.Success -> data = r.data
            is NetworkResult.Error -> error = r.message
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            val d = data
            val views = d?.viewCount ?: story.viewCount ?: 0
            val shots = d?.screenshotCount ?: story.screenshotCount ?: 0
            val likes = d?.reactionCount ?: story.reactionCount ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Visibility, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(S.plural(R.plurals.story_views_count, views), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (likes > 0) {
                    Spacer(Modifier.width(14.dp))
                    Text("❤️", fontSize = 16.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(S.plural(R.plurals.story_reactions_count, likes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (shots > 0) {
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Filled.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(S.plural(R.plurals.story_screenshots_count, shots), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                d == null && error == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                d != null && (d.isPremiumRequired || !isPremium) -> Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.WorkspacePremium, null, tint = Color(0xFFFFB300), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(S.plural(R.plurals.story_viewers_locked_title, views), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        S.get(R.string.story_viewers_locked_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth()) { Text(S.get(R.string.story_subscribe)) }
                }
                d != null && d.viewers.isEmpty() -> Text(
                    S.get(R.string.story_no_viewers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
                d != null -> LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(d.viewers, key = { it.viewer?.id ?: it.viewedAt.orEmpty() }) { item ->
                        val v = item.viewer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HalaAsyncImage(
                                model = v?.profileImage,
                                contentDescription = null,
                                fallbackName = v?.name,
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(v?.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                                    if (v?.isVerified == true) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Filled.Verified, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(15.dp))
                                    }
                                }
                                Text(
                                    NotificationFormat.timeAgo(item.viewedAt),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item.reaction?.let { Text(it, fontSize = 20.sp) }
                            if (item.screenshotAt != null) {
                                Spacer(Modifier.width(6.dp))
                                Text("📸", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** «حفظ في الصور» لقصتي المصوّرة — MediaStore بلا صلاحية (API 29+). */
object StoryMediaSaver {
    suspend fun save(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = ApiClient.httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    check(resp.isSuccessful)
                    resp.body!!.bytes()
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "hala_story_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hala")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                resolver.openOutputStream(uri)!!.use { it.write(bytes) }
            }.isSuccess
        }
        context.showToast(S.get(if (ok) R.string.story_saved_to_photos else R.string.story_save_failed))
    }
}
