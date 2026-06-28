package com.chathala.hala.core.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chathala.hala.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * يحمّل إعلان AdMob مدمج (Native) ويُعيده عند الجاهزية (أو null أثناء التحميل/الفشل).
 * يُدمَّر تلقائياً عند الخروج من التركيب.
 */
@Composable
fun rememberNativeAd(): NativeAd? {
    val context = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val loader = com.google.android.gms.ads.AdLoader.Builder(context, AdConfig.nativeUnitId)
            .forNativeAd { loaded ->
                ad?.destroy()
                ad = loaded
            }
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { ad?.destroy() }
    }
    return ad
}

/**
 * عنصر قائمة جاهز للإدماج بين بطاقات المستخدمين:
 *  - يحمّل إعلاناً مدمجاً خاصاً به.
 *  - عند الجاهزية: وسم «إعلان» صغير + بطاقة الإعلان بنمط التطبيق.
 *  - أثناء التحميل/الفشل: لا يعرض شيئاً (لا يكسر تدفّق القائمة).
 */
@Composable
fun NativeAdListItem(modifier: Modifier = Modifier) {
    val ad = rememberNativeAd() ?: return
    // بطاقة بنمط بطاقة المستخدم (صف مدمج) — الشارة «إعلان» داخل التخطيط
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        NativeAdRowCard(nativeAd = ad)
    }
}

/** إعلان مدمج بشكل صف يشبه بطاقة المستخدم (أيقونة + عنوان + وصف + زر). */
@Composable
fun NativeAdRowCard(nativeAd: NativeAd, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val view = LayoutInflater.from(ctx)
                .inflate(R.layout.ad_native_row, null) as NativeAdView
            bindRow(view, nativeAd)
            view
        },
        update = { bindRow(it, nativeAd) }
    )
}

private fun bindRow(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val icon = view.findViewById<ImageView>(R.id.ad_app_icon)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)

    headline.text = ad.headline
    view.headlineView = headline

    if (ad.body != null) {
        body.text = ad.body
        body.visibility = android.view.View.VISIBLE
    } else {
        body.visibility = android.view.View.GONE
    }
    view.bodyView = body

    val adIcon = ad.icon
    if (adIcon?.drawable != null) {
        icon.setImageDrawable(adIcon.drawable)
    } else {
        // بديل: أول صورة من الوسائط إن لم تتوفّر أيقونة
        ad.images.firstOrNull()?.drawable?.let { icon.setImageDrawable(it) }
    }
    view.iconView = icon

    if (ad.callToAction != null) {
        cta.text = ad.callToAction
        cta.visibility = android.view.View.VISIBLE
    } else {
        cta.visibility = android.view.View.GONE
    }
    view.callToActionView = cta

    view.setNativeAd(ad)
}

/** يعرض إعلاناً مدمجاً داخل بطاقة بنمط التطبيق. */
@Composable
fun NativeAdCard(nativeAd: NativeAd, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val view = LayoutInflater.from(ctx)
                .inflate(R.layout.ad_native_card, null) as NativeAdView
            bind(view, nativeAd)
            view
        },
        update = { bind(it, nativeAd) }
    )
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val icon = view.findViewById<ImageView>(R.id.ad_app_icon)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)
    val media = view.findViewById<MediaView>(R.id.ad_media)

    headline.text = ad.headline
    view.headlineView = headline

    if (ad.body != null) {
        body.text = ad.body
        body.visibility = android.view.View.VISIBLE
    } else {
        body.visibility = android.view.View.GONE
    }
    view.bodyView = body

    val adIcon = ad.icon
    if (adIcon != null) {
        icon.setImageDrawable(adIcon.drawable)
        icon.visibility = android.view.View.VISIBLE
    } else {
        icon.visibility = android.view.View.GONE
    }
    view.iconView = icon

    if (ad.callToAction != null) {
        cta.text = ad.callToAction
        cta.visibility = android.view.View.VISIBLE
    } else {
        cta.visibility = android.view.View.GONE
    }
    view.callToActionView = cta

    view.mediaView = media
    ad.mediaContent?.let { media.mediaContent = it }

    view.setNativeAd(ad)
}
