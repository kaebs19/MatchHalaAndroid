package com.chathala.hala.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.storage.ChatWallpaper
import com.chathala.hala.ui.components.chatWallpaper

/** معرّف المورد لا النص — الـ enum يتجمّد على لغة الإقلاع لو خزّن النص. */
val ChatWallpaper.labelRes: Int
    get() = when (this) {
        ChatWallpaper.HEARTS -> R.string.chat_wallpaper_hearts
        ChatWallpaper.NONE -> R.string.chat_wallpaper_none
        ChatWallpaper.STARS -> R.string.chat_wallpaper_stars
        ChatWallpaper.BUBBLES -> R.string.chat_wallpaper_bubbles
        ChatWallpaper.DOTS -> R.string.chat_wallpaper_dots
        ChatWallpaper.WAVES -> R.string.chat_wallpaper_waves
    }

/** ورقة اختيار خلفية المحادثة — شبكة معاينات حيّة بثلاثة أعمدة. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWallpaperPickerSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModel.Factory
    )
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current by viewModel.chatWallpaper.collectAsState(initial = ChatWallpaper.HEARTS)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = S.get(R.string.settings_chat_wallpaper_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = S.get(R.string.settings_chat_wallpaper_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            ChatWallpaper.entries.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { wallpaper ->
                        WallpaperTile(
                            wallpaper = wallpaper,
                            selected = wallpaper == current,
                            onClick = { viewModel.setChatWallpaper(wallpaper) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WallpaperTile(
    wallpaper: ChatWallpaper,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(shape)
                .border(if (selected) 2.dp else 1.dp, borderColor, shape)
                .clickable(onClick = onClick)
                .chatWallpaper(wallpaper, scale = 0.55f)
        ) {
            // فقاعتان مصغّرتان تُظهران كيف يبدو النقش خلف الرسائل فعلاً
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .width(52.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp, 2.dp, 8.dp, 8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .width(40.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp, 8.dp, 8.dp, 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Text(
            text = S.get(wallpaper.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
