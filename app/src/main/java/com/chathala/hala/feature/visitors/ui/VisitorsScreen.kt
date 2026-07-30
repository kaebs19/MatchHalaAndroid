package com.chathala.hala.feature.visitors.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chathala.hala.core.data.Countries
import com.chathala.hala.feature.notifications.util.NotificationFormat
import com.chathala.hala.feature.visitors.data.ProfileViewItem
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaPrimaryButton

private val Gold = Color(0xFFFFC107)
private val GoldDark = Color(0xFFFFA726)

@Composable
fun VisitorsScreen(
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: VisitorsViewModel = viewModel(factory = VisitorsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(onBack = onBack, total = state.totalViews)

        when {
            state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.error != null && state.views.isEmpty() ->
                ErrorState(message = state.error ?: "", onRetry = viewModel::load)

            state.views.isEmpty() -> EmptyVisitors()

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.premiumRequired) {
                    item(key = "upgrade") { UpgradeBanner(total = state.totalViews, onClick = onOpenPremium) }
                }
                itemsIndexed(state.views, key = { i, v -> v.viewer?.id ?: "hidden_$i" }) { _, item ->
                    VisitorRow(
                        item = item,
                        locked = state.premiumRequired,
                        onClick = {
                            val id = item.viewer?.id
                            if (state.premiumRequired || id == null) onOpenPremium()
                            else onOpenUserProfile(id)
                        }
                    )
                }
                if (state.canLoadMore) {
                    item(key = "load_more") {
                        LaunchLoadMore(onLoadMore = viewModel::loadMore)
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun LaunchLoadMore(onLoadMore: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onLoadMore() }
    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = "زوّار ملفك",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (total > 0) {
                Text(
                    text = "$total زيارة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** بانر ذهبي للمجاني: اكشف من زار ملفك. */
@Composable
private fun UpgradeBanner(total: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Gold, GoldDark)))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (total > 0) "$total شخصاً زاروا ملفك" else "اكشف زوّار ملفك",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4A2C00)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "اشترك في بريميوم لمعرفة من زارك",
                fontSize = 12.5.sp,
                color = Color(0xFF5A3A00)
            )
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF3A2400))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("ترقية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE082))
        }
    }
}

@Composable
private fun VisitorRow(item: ProfileViewItem, locked: Boolean, onClick: () -> Unit) {
    val visitor = item.viewer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // الأفاتار — مموّه للمجاني
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!locked && !visitor?.profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = visitor?.profileImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.Person, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp).then(if (locked) Modifier.blur(6.dp) else Modifier)
                )
            }
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (locked) {
                // اسم مموّه للمجاني
                Box(
                    modifier = Modifier
                        .width(110.dp).height(15.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                        .blur(3.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = visitor?.name ?: "مستخدم",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    if (visitor?.isVerified == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp))
                    }
                }
                Countries.byCode(visitor?.country)?.let { c ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${c.flag} ${c.nameAr}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = NotificationFormat.timeAgoArabic(item.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (locked) {
            Icon(Icons.Filled.Lock, null, tint = Gold, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptyVisitors() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Visibility, null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "لا يوجد زوّار بعد",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "عندما يزور أحدهم ملفك سيظهر هنا.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
