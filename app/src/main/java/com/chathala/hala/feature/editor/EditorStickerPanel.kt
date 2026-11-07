package com.chathala.hala.feature.editor

import android.Manifest
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.LocaleManager
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.util.LocationHelper
import com.chathala.hala.core.util.showToast
import com.chathala.hala.feature.stickers.StickerCell
import com.chathala.hala.feature.stickers.StickerLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAB_EMOJI = "emoji"
private const val TAB_INFO = "info"

/**
 * درج ملصقات المحرّر: إيموجي، و«📍 موقع وحالة» للقصص فقط، وفئات ملصقات لوحة التحكم
 * (فئة هلا+ مقفلة لغير المشترك). [onAdd] يستلم الملصق جاهزاً (صورته محمّلة للرسم).
 */
@Composable
internal fun EditorStickerPanel(
    mode: EditorMode,
    emojis: List<String>,
    onAdd: (Sticker) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as HalaApp
    val user by app.userRepository.currentUser.collectAsState(initial = null)
    val isPremium = user?.isPremium == true
    val categories by StickerLibrary.categories.collectAsState()
    var tab by rememberSaveable { mutableStateOf(TAB_EMOJI) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { StickerLibrary.refresh() }

    fun addLocation() {
        busy = true
        scope.launch {
            val place = runCatching {
                val me = LocationHelper.fetchLastLocation(context) ?: return@runCatching null
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    Geocoder(context, LocaleManager.locale).getFromLocation(me.lat, me.lng, 1)?.firstOrNull()
                        ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
                }
            }.getOrNull()
            busy = false
            if (place.isNullOrBlank()) {
                context.showToast(S.get(R.string.sticker_city_failed))
                return@launch
            }
            val keys = InfoStickers.location(place).map { it.first }
            context.showToast(S.get(R.string.sticker_tap_to_restyle))
            onAdd(Sticker(System.nanoTime(), "", y = 0.7f, size = 0.45f, imageKey = keys.first(), variants = keys, locationPlace = place))
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) addLocation()
        else context.showToast(S.get(R.string.sticker_location_denied))
    }

    Column {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PanelTab(S.get(R.string.sticker_tab_emoji), tab == TAB_EMOJI) { tab = TAB_EMOJI }
            if (mode == EditorMode.STORY) PanelTab(S.get(R.string.sticker_tab_info), tab == TAB_INFO) { tab = TAB_INFO }
            categories.forEach { c ->
                PanelTab((if (c.premium == true) "👑 " else "") + c.name, tab == c.id) { tab = c.id }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            val category = categories.firstOrNull { it.id == tab }
            when {
                tab == TAB_INFO -> LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize()) {
                    item(span = { GridItemSpan(3) }) {
                        Row(
                            Modifier
                                .padding(4.dp)
                                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                .clickable(enabled = !busy) {
                                    if (LocationHelper.hasLocationPermission(context)) addLocation()
                                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("📍", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(S.get(R.string.sticker_my_city), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    items(StoryMoods) { m ->
                        Row(
                            Modifier
                                .padding(4.dp)
                                .background(Color.White.copy(alpha = 0.10f), CircleShape)
                                .clickable {
                                    val keys = InfoStickers.mood(m).map { it.first }
                                    context.showToast(S.get(R.string.sticker_tap_to_restyle))
                                    onAdd(Sticker(System.nanoTime(), "", y = 0.3f, size = 0.5f, imageKey = keys.first(), variants = keys))
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(S.get(m.labelRes), color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                category != null -> {
                    val locked = category.premium == true && !isPremium
                    LazyVerticalGrid(GridCells.Fixed(5), Modifier.fillMaxSize()) {
                        items(category.stickers, key = { it.id }) { s ->
                            StickerCell(s, locked) {
                                if (locked) {
                                    context.showToast(S.get(R.string.sticker_premium_category))
                                    return@StickerCell
                                }
                                scope.launch {
                                    if (StickerImageStore.load(context, s.imageUrl) == null) {
                                        context.showToast(S.get(R.string.sticker_unavailable))
                                    } else onAdd(Sticker(System.nanoTime(), "", size = 0.32f, imageKey = s.imageUrl))
                                }
                            }
                        }
                    }
                }
                else -> LazyVerticalGrid(GridCells.Adaptive(44.dp), Modifier.fillMaxSize()) {
                    items(emojis) { e ->
                        Text(e, fontSize = 28.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { onAdd(Sticker(System.nanoTime(), e)) }.padding(6.dp))
                    }
                }
            }
        }
    }
}


@Composable
private fun PanelTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(30.dp)
            .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}
