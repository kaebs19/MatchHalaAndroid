package com.chathala.hala.core.ads

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun rememberNativeAd(slot: String): NativeAd? {
    if (!AdGate.rememberEnabled()) return null
    val activity = LocalContext.current.findActivity() ?: return null
    val state = NativeAdPool.slotOf(slot)
    LifecycleResumeEffect(slot, activity) {
        NativeAdPool.attach(activity, slot)
        onPauseOrDispose { NativeAdPool.detach(slot, activity) }
    }
    return state.ad
}

fun refreshNativeAd(slot: String) = NativeAdPool.invalidate(slot)

/** Full-width sponsored cards keep disclosure, media, AdChoices and CTA readable. */
@Composable
fun NativeAdListItem(slot: String, modifier: Modifier = Modifier) {
    if (!AdGate.rememberEnabled()) return
    val ad = rememberNativeAd(slot)
    if (ad == null) {
        if (NativeAdPool.slotOf(slot).loading) Box(modifier.fillMaxWidth().height(280.dp)
            .clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)))
        return
    }
    NativeAdCard(ad, modifier)
}

@Composable
fun NativeAdGridItem(slot: String, modifier: Modifier = Modifier) = NativeAdListItem(slot, modifier)

@Composable
fun NativeAdCard(nativeAd: NativeAd, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val label = S.get(R.string.ad_label)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AndroidView(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp)),
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.ad_native_card, null) as NativeAdView).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        },
        update = { view ->
            view.layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            view.setBackgroundColor(colors.surface.toArgb())
            view.findViewById<TextView>(R.id.ad_attribution).apply {
                text = label
                setTextColor(colors.onPrimary.toArgb())
                backgroundTintList = ColorStateList.valueOf(colors.primary.toArgb())
            }
            val headline = view.findViewById<TextView>(R.id.ad_headline)
            val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
            val body = view.findViewById<TextView>(R.id.ad_body)
            val icon = view.findViewById<ImageView>(R.id.ad_app_icon)
            val cta = view.findViewById<Button>(R.id.ad_call_to_action)
            val media = view.findViewById<MediaView>(R.id.ad_media)
            headline.setTextColor(colors.onSurface.toArgb())
            body.setTextColor(colors.onSurfaceVariant.toArgb())
            advertiser.setTextColor(colors.onSurfaceVariant.toArgb())
            cta.setTextColor(colors.onPrimary.toArgb())
            cta.backgroundTintList = ColorStateList.valueOf(colors.primary.toArgb())
            if (view.tag !== nativeAd) {
                headline.text = nativeAd.headline
                advertiser.text = nativeAd.advertiser
                advertiser.visibility = if (nativeAd.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
                body.text = nativeAd.body
                body.visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
                icon.setImageDrawable(nativeAd.icon?.drawable)
                icon.visibility = if (nativeAd.icon?.drawable == null) View.GONE else View.VISIBLE
                cta.text = nativeAd.callToAction
                cta.visibility = if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
                media.mediaContent = nativeAd.mediaContent
                media.visibility = if (nativeAd.mediaContent == null) View.GONE else View.VISIBLE
                view.headlineView = headline
                view.advertiserView = advertiser
                view.bodyView = body
                view.iconView = icon
                view.callToActionView = cta
                view.mediaView = media
                view.setNativeAd(nativeAd)
                view.tag = nativeAd
            }
        },
        onRelease = { it.destroy() }
    )
}
