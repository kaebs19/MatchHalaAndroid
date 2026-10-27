package com.chathala.hala.feature.stories.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import coil.compose.AsyncImage
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.util.showToast
import com.chathala.hala.feature.stories.data.Story
import com.chathala.hala.ui.components.HalaAsyncImage
import kotlinx.coroutines.launch

private val Gold = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000)))

/**
 * بطاقة «قصصي» في «ملفي» (بعد الإحصائيات): ترويسة بأيقونة دائرية وسطر فرعي و«عرض الكل»،
 * ثم شريط أفقي ببطاقات 80×130 وبطاقة (+) في آخره هي زرّ الإضافة الوحيد. المجاني عند
 * الحدّ ← (+) ذهبية بتاج.
 */
@Composable
fun MyStoriesCard(
    myImage: String?,
    myName: String,
    isPremium: Boolean,
    onAdd: () -> Unit,
    onOpenStory: (Story?) -> Unit,
    modifier: Modifier = Modifier
) {
    val manager = (LocalContext.current.applicationContext as HalaApp).storyManager
    val stories by manager.myStories.collectAsState()
    val uploads by manager.uploads.collectAsState()
    val limits by manager.limits.collectAsState()
    var showAll by remember { mutableStateOf(false) }
    val atLimit = stories.size >= limits.max

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StoryRingBox(
                state = if (stories.isEmpty()) StoryRingState.None else StoryRingState.Unseen,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { if (stories.isEmpty()) onAdd() else onOpenStory(stories.lastOrNull()) },
                ringWidth = 2.dp,
                gap = 1.5.dp,
                gapColor = MaterialTheme.colorScheme.surface
            ) {
                HalaAsyncImage(model = myImage, contentDescription = null, fallbackName = myName, modifier = Modifier.fillMaxSize().clip(CircleShape))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(S.get(R.string.story_my_stories), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (stories.isEmpty()) S.get(R.string.story_none_active)
                    else S.plural(R.plurals.story_active_count, stories.size) + " · " + S.get(R.string.story_disappear_after_24h),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (stories.isNotEmpty()) {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { showAll = true }.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(S.get(R.string.story_view_all), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (stories.isEmpty() && uploads.isEmpty()) {
            EmptyStories(myImage, myName, onAdd)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(stories, key = { it.id }) { story ->
                    StoryThumb(story, Modifier.clickable { onOpenStory(story) })
                }
                items(uploads, key = { "up-${it.id}" }) { up ->
                    Box(
                        Modifier.size(80.dp, 130.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(progress = { up.progress }, modifier = Modifier.size(34.dp), strokeWidth = 3.dp) }
                }
                item(key = "add") {
                    AddThumb(gold = atLimit && !isPremium, onClick = onAdd)
                }
            }
        }
    }

    if (showAll) {
        AllStoriesSheet(
            stories = stories,
            max = limits.max,
            onOpen = { showAll = false; onOpenStory(it) },
            onDismiss = { showAll = false }
        )
    }
}

@Composable
private fun EmptyStories(myImage: String?, myName: String, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            val color = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = color,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
                )
            }
            HalaAsyncImage(model = myImage, contentDescription = null, fallbackName = myName, modifier = Modifier.size(54.dp).clip(CircleShape))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Button(onClick = onAdd, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(S.get(R.string.story_add))
            }
            Spacer(Modifier.height(4.dp))
            Text(S.get(R.string.story_empty_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StoryThumb(story: Story, modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp = 80.dp) {
    val h = width * 13 / 8
    Box(
        modifier
            .size(width, h)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(2.dp, StoryRingBrush), RoundedCornerShape(12.dp))
    ) {
        if (story.isText) {
            TextStoryCanvas(story.text.orEmpty(), story.background, Modifier.fillMaxSize(), fontSize = 10.sp, maxLines = 5)
        } else {
            AsyncImage(model = story.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (story.isHeld) {
            Text(
                S.get(R.string.story_held_badge),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xE6B26A00))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                .padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shots = story.screenshotCount ?: 0
            Text(
                "👁 ${story.viewCount ?: 0}" + if (shots > 0) " 📸 $shots" else "",
                color = Color.White,
                fontSize = 9.sp
            )
            story.expiresAtMillis?.let { exp ->
                val hours = ((exp - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(0)
                Text(S.get(R.string.story_hours_left, hours.toInt()), color = Color.White, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun AddThumb(gold: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .size(80.dp, 130.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (gold) Modifier.background(Gold)
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (gold) Icon(Icons.Filled.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (gold) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, S.get(R.string.story_add), tint = Color.White) }
        if (gold) {
            Spacer(Modifier.height(4.dp))
            Text(S.get(R.string.story_add_more_premium), color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllStoriesSheet(stories: List<Story>, max: Int, onOpen: (Story) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = (context.applicationContext as HalaApp).storyManager
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Story?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                S.get(R.string.story_all_title, stories.size, max),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(520.dp)
            ) {
                items(stories, key = { it.id }) { story ->
                    Box(Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)).clickable { onOpen(story) }) {
                        if (story.isText) TextStoryCanvas(story.text.orEmpty(), story.background, Modifier.fillMaxSize(), fontSize = 12.sp, maxLines = 6)
                        else AsyncImage(model = story.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { confirm = story },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Delete, S.get(R.string.story_delete), tint = Color.White, modifier = Modifier.size(17.dp)) }
                    }
                }
            }
        }
    }
    confirm?.let { story ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(S.get(R.string.story_delete_confirm_title)) },
            text = { Text(S.get(R.string.story_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        when (val r = manager.delete(story.id)) {
                            is NetworkResult.Success -> context.showToast(S.get(R.string.story_deleted))
                            is NetworkResult.Error -> context.showToast(r.message)
                        }
                    }
                }) { Text(S.get(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(S.get(R.string.action_cancel)) } }
        )
    }
}
