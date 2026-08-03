package com.chathala.hala.core.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * بانر AdMob متجاوب (Adaptive anchored) بعرض الشاشة.
 * يُدمَّر تلقائياً عند خروجه من التركيب.
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerUnitId
) {
    if (!AdGate.rememberEnabled()) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adaptiveSize(ctx))
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() = AdLog.loaded("بانر")
                    override fun onAdFailedToLoad(error: LoadAdError) =
                        AdLog.failure("بانر", error)
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() }
    )
}

private fun adaptiveSize(context: Context): AdSize {
    val metrics = context.resources.displayMetrics
    val widthDp = (metrics.widthPixels / metrics.density).toInt().coerceAtLeast(320)
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
}
