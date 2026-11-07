package com.chathala.hala.feature.stickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.util.showToast

private const val TAB_EMOJI = "emoji"
private const val TAB_FAVORITES = "favorites"

private val PanelEmojis = listOf(
    "😊", "😂", "🤣", "😍", "🥰", "😘", "😉", "😎", "🤩", "😇", "🙈", "🥺",
    "😢", "😭", "😤", "😡", "😴", "🤔", "🙄", "😳", "🤗", "🤭", "❤️", "🧡",
    "💛", "💚", "💙", "💜", "🖤", "💔", "💕", "🔥", "✨", "🎉", "🌹", "👍",
    "👏", "🙏", "💪", "👋"
)

/**
 * لوحة زرّ الإيموجي (مقابل `ChatStickerPanel` في iOS): تبويب الإيموجي يكتب في الحقل،
 * و«⭐ المفضلة» (المفضلة + الأخيرة) وفئات الملصقات تُرسل مباشرة. فئة هلا+ مقفلة لغير المشترك.
 * [onSticker] null = إيموجي فقط.
 */
@Composable
fun ChatStickerPanel(
    isPremium: Boolean,
    onEmoji: (String) -> Unit,
    onSticker: ((StickerItem) -> Unit)?,
    onPremiumRequired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by StickerLibrary.categories.collectAsState()
    val favoriteIds by StickerLibrary.favoriteIds.collectAsState()
    val recentIds by StickerLibrary.recentIds.collectAsState()
    var tab by rememberSaveable { mutableStateOf(TAB_EMOJI) }
    val app = context.applicationContext as com.chathala.hala.HalaApp
    val user by app.userRepository.currentUser.collectAsState(initial = null)
    LaunchedEffect(user?.id) { StickerLibrary.setOwner(user?.id) }
    LaunchedEffect(Unit) { StickerLibrary.refresh() }

    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (onSticker != null) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Tab(S.get(R.string.sticker_tab_emoji), tab == TAB_EMOJI) { tab = TAB_EMOJI }
                Tab(S.get(R.string.sticker_tab_favorites), tab == TAB_FAVORITES) { tab = TAB_FAVORITES }
                categories.forEach { c ->
                    Tab((if (c.premium == true) "👑 " else "") + c.name, tab == c.id) { tab = c.id }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.fillMaxWidth().height(240.dp)) {
            val category = categories.firstOrNull { it.id == tab }
            when {
                tab == TAB_FAVORITES && onSticker != null -> {
                    val favorites = StickerLibrary.items(favoriteIds, isPremium)
                    val recents = StickerLibrary.items(recentIds, isPremium)
                    if (favorites.isEmpty() && recents.isEmpty()) {
                        Text(
                            S.get(R.string.sticker_favorites_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                    } else LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize()) {
                        if (favorites.isNotEmpty()) {
                            item(span = { GridItemSpan(4) }) { SectionTitle(S.get(R.string.sticker_section_favorites)) }
                            items(favorites, key = { "f" + it.id }) { StickerCell(it, false) { onSticker(it) } }
                        }
                        if (recents.isNotEmpty()) {
                            item(span = { GridItemSpan(4) }) { SectionTitle(S.get(R.string.sticker_section_recent)) }
                            items(recents, key = { "r" + it.id }) { StickerCell(it, false) { onSticker(it) } }
                        }
                    }
                }
                category != null && onSticker != null -> {
                    val locked = category.premium == true && !isPremium
                    LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize()) {
                        items(category.stickers, key = { it.id }) { s ->
                            StickerCell(s, locked) {
                                if (locked) {
                                    context.showToast(S.get(R.string.sticker_premium_category))
                                    onPremiumRequired()
                                } else onSticker(s)
                            }
                        }
                    }
                }
                else -> LazyVerticalGrid(GridCells.Fixed(7), Modifier.fillMaxSize()) {
                    items(PanelEmojis) { e ->
                        Box(
                            Modifier.aspectRatio(1f).clip(CircleShape).clickable { onEmoji(e) },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 28.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tab(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun StickerCell(sticker: StickerItem, locked: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = sticker.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp).alpha(if (locked) 0.45f else 1f)
        )
        if (locked) Text("👑", fontSize = 16.sp, modifier = Modifier.align(Alignment.TopEnd))
    }
}
