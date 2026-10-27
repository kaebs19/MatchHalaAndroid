package com.chathala.hala.core.ads

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Measured adaptive banner; failed/unfilled placements occupy no empty panel. */
@Composable
fun BannerAd(
    slot: String,
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerUnitId,
    /** هوامش تُطبَّق فقط حين يظهر إعلان — لا شريط فارغ قبل وصوله. */
    loadedPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues()
) {
    if (!AdGate.rememberEnabled()) return
    val activity = LocalContext.current.findActivity() ?: return
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // عرض الإعلان = المتاح ناقص الهوامش التي تُطبَّق عند ظهوره
        val ld = androidx.compose.ui.platform.LocalLayoutDirection.current
        val usable = maxWidth - loadedPadding.calculateLeftPadding(ld) - loadedPadding.calculateRightPadding(ld)
        if (!usable.value.isFinite() || usable.value < 320) return@BoxWithConstraints
        val width = usable.value.toInt()
        key(slot, width, activity, adUnitId) {
            val state = BannerAdPool.stateOf(slot)
            val height = if (state?.loaded == true) with(density) {
                (state.view.adSize?.getHeightInPixels(activity) ?: 0).toDp()
            } else 0.dp
            LifecycleResumeEffect(slot, activity, width) {
                BannerAdPool.resume(slot)
                onPauseOrDispose { BannerAdPool.pause(slot) }
            }
            AndroidView(
                modifier = Modifier
                    .then(if (state?.loaded == true) Modifier.padding(loadedPadding) else Modifier)
                    .fillMaxWidth()
                    .height(height),
                factory = { BannerAdPool.obtain(activity, slot, adUnitId, width) },
                onRelease = { BannerAdPool.release(slot, it) }
            )
        }
    }
}
