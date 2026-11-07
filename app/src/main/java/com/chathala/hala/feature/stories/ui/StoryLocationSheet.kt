package com.chathala.hala.feature.stories.ui

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.util.LocationHelper
import com.chathala.hala.feature.stories.data.StoryLocationTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ورقة ملصق المدينة في القصة (مقابل `StoryLocationCard` في iOS): الاسم، والمسافة التقريبية
 * إن كان المشاهد أذن بالموقع مسبقاً (لا طلب إذن هنا)، و«فتح في الخرائط».
 * لا إحداثيات من الناشر أبداً — المدينة تُحوَّل إلى موقع على جهاز المشاهد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryLocationSheet(tag: StoryLocationTag, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var distance by remember { mutableStateOf<String?>(null) }
    var city by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(tag.place) {
        val point = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, com.chathala.hala.core.i18n.LocaleManager.locale)
                    .getFromLocationName(tag.place, 1)?.firstOrNull()
                    ?.let { it.latitude to it.longitude }
            }.getOrNull()
        } ?: return@LaunchedEffect
        city = point
        if (!LocationHelper.hasLocationPermission(context)) return@LaunchedEffect
        val me = runCatching { LocationHelper.fetchLastLocation(context) }.getOrNull() ?: return@LaunchedEffect
        val out = FloatArray(1)
        android.location.Location.distanceBetween(me.lat, me.lng, point.first, point.second, out)
        val km = out[0] / 1000.0
        distance = if (km < 30) S.get(R.string.story_location_near)
        else S.get(R.string.story_location_km, ((km / 10).toInt() * 10).coerceAtLeast(30))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text(tag.place, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
            distance?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val c = city
                    val uri = if (c != null) Uri.parse("geo:${c.first},${c.second}?q=${Uri.encode(tag.place)}")
                    else Uri.parse("geo:0,0?q=${Uri.encode(tag.place)}")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(tag.place)}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.Map, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.get(R.string.story_location_open_maps), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
