package com.chathala.hala.core.ads

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chathala.hala.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * يُعيد الإعلان المدمج الخاص بالخانة [slot]، أو null أثناء التحميل/الفشل.
 *
 * الإعلان محفوظ في [NativeAdPool]: يبقى حيّاً بعد خروج العنصر من التركيب (تمرير
 * القائمة أو مغادرة الشاشة)، فيظهر فوراً عند العودة بدل إعادة الطلب من الصفر.
 *
 * @param slot مفتاح ثابت وفريد للموضع — موضعان ظاهران معاً يجب أن يختلف مفتاحاهما،
 *   إذ لا يجوز ربط نفس `NativeAd` بأكثر من `NativeAdView` في آن واحد.
 */
@Composable
fun rememberNativeAd(slot: String): NativeAd? {
    val context = LocalContext.current
    if (!AdGate.rememberEnabled()) return null
    DisposableEffect(slot) {
        NativeAdPool.attach(context, slot)
        onDispose { NativeAdPool.detach(slot) }
    }
    // القراءة من حالة الخانة (تُنشأ فارغة إن لزم) حتى يُعاد التركيب عند وصول الإعلان.
    return NativeAdPool.slotOf(slot).ad
}

/** هل ما زالت خانة الإعلان المدمج تنتظر رداً؟ — لحجز مكانه بدل قفز التخطيط. */
@Composable
private fun nativeAdLoading(slot: String): Boolean = NativeAdPool.slotOf(slot).loading

/** يُسقِط إعلان الخانة ليُطلب غيره في المرة القادمة (لموضع يُعرض مراراً). */
fun refreshNativeAd(slot: String) = NativeAdPool.invalidate(slot)

/**
 * عنصر قائمة جاهز للإدماج بين بطاقات المستخدمين:
 *  - يعرض إعلان الخانة [slot] عند جاهزيته: بطاقة بنمط التطبيق مع وسم «إعلان».
 *  - أثناء التحميل: مكان محجوز بارتفاع الصف (لا تقفز القائمة عند وصول الإعلان).
 *  - عند الفشل: لا يعرض شيئاً (لا يترك فراغاً في تدفّق القائمة).
 */
@Composable
fun NativeAdListItem(slot: String, modifier: Modifier = Modifier) {
    val ad = rememberNativeAd(slot)
    if (ad == null) {
        if (nativeAdLoading(slot)) AdPlaceholder(modifier.fillMaxWidth().height(NATIVE_ROW_HEIGHT))
        return
    }
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

/** ارتفاع تقريبي لصف الإعلان المدمج — يُستعمل للمكان المحجوز أثناء التحميل فقط. */
private val NATIVE_ROW_HEIGHT = 88.dp

/** مكان محجوز محايد أثناء انتظار الإعلان — بلا نصّ أو وسم حتى لا يُقرأ كإعلان. */
@Composable
private fun AdPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    )
}

/**
 * إعلان مدمج بشكل **بطاقة** بأبعاد بطاقة المستخدم في الشبكة.
 *
 * لا يُعاد استخدام [NativeAdListItem] هنا: تخطيطه أفقي (أيقونة + نصّ + زر على
 * سطر واحد) فيبدو شبه فارغ داخل خانة 3:4، ولا `ad_native_card` لأن ارتفاعه
 * الثابت يتجاوز الخانة فتُقصّ عناصر الإعلان خارج حدود NativeAdView — وهي
 * مخالفة يرصدها AdMob native ad validator. `ad_native_grid` يملأ الخانة تماماً.
 */
@Composable
fun NativeAdGridItem(slot: String, modifier: Modifier = Modifier) {
    val ad = rememberNativeAd(slot)
    if (ad == null) {
        if (nativeAdLoading(slot)) AdPlaceholder(modifier.aspectRatio(0.75f))
        return
    }
    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        NativeAdGridCard(nativeAd = ad, modifier = Modifier.fillMaxSize())
    }
}

/** إعلان مدمج يملأ خانة الشبكة — الوسائط مرنة والزر داخل الحدود دائماً. */
@Composable
private fun NativeAdGridCard(nativeAd: NativeAd, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val view = LayoutInflater.from(ctx)
                .inflate(R.layout.ad_native_grid, null) as NativeAdView
            // بدون layoutParams صريحة تبقى wrap_content فيتجاوز المحتوى الخانة
            view.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            bind(view, nativeAd)
            view
        },
        update = { bind(it, nativeAd) }
    )
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
