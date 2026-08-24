package com.chathala.hala.core.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * بانر AdMob متجاوب (Adaptive anchored) بعرض الموضع الذي يشغله.
 *
 * الـ `AdView` محفوظ في [BannerAdPool] تحت [slot]، فلا يُدمَّر عند التمرير خارج الشاشة
 * أو مغادرتها: عند العودة يُعاد إرفاق نفس الإعلان بدل طلب إعلان جديد قد يُقابَل بـ
 * no fill فيبقى الموضع فارغاً.
 *
 * @param slot مفتاح ثابت وفريد لهذا الموضع على الشاشة. موضعان ظاهران معاً يجب أن
 *   يحملا مفتاحين مختلفين — الـ view الواحد لا يُرفَق بأبوين في آن.
 */
@Composable
fun BannerAd(
    slot: String,
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerUnitId
) {
    if (!AdGate.rememberEnabled()) return
    val context = LocalContext.current
    // العرض يُقاس من التخطيط نفسه لا من `displayMetrics`.
    //
    // القياس السابق `displayMetrics.widthPixels / density` كان يردّ **ارتفاع** الشاشة
    // أحياناً (رُصد على الجهاز: تذبذب 832dp ↔ 384dp على هاتف 1080×2340 بكثافة 2.8125)
    // فيُطلب بانر متجاوب بعرض 832dp لا وجود له على هذا الجهاز — ثم يتبدّل الرقم
    // فيهدم المخزن الـ AdView ويعيد بناءه، ومعه طلب جديد. أي: مقاس خاطئ يُقابَل
    // بـ no fill، وإعلان قائم يُفقَد عند كل تذبذب.
    //
    // وهذا أيضاً ما توصي به Google للبانر المتجاوب: عرض **الحاوية** لا عرض الشاشة —
    // وهما يختلفان هنا فعلاً بمقدار الحشو الأفقي.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(MIN_BANNER_WIDTH_DP)
        // تشخيص: رُصد على الجهاز عرض مقيس 752dp على شاشة رأسية عرضها 384dp — وهو
        // ارتفاع منطقة المحتوى. مقارنته بعرض الإعداد تكشف إن كانت النافذة تُبلّغ
        // اتجاهاً أفقياً لحظياً أم أن قيود التخطيط وحدها هي الشاذّة.
        val configWidthDp = LocalConfiguration.current.screenWidthDp
        LaunchedEffect(slot, widthDp) {
            AdLog.slot("«$slot» عرض مقيس=${widthDp}dp عرض الإعداد=${configWidthDp}dp")
        }
        // المفتاح يحمل العرض: تغيّره الحقيقي (دوران، تعدّد نوافذ) يُعيد البناء بمقاس
        // صحيح، ولولاه لبقي `factory` على مقاسه الأول لأنها لا تُعاد عند تغيّر الوسائط.
        key(slot, widthDp) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { BannerAdPool.obtain(context, slot, adUnitId, widthDp) },
                onRelease = { BannerAdPool.release(slot) }
            )
        }
    }
}

/** أضيق عرض يقبله البانر المتجاوب لدى AdMob. */
private const val MIN_BANNER_WIDTH_DP = 320
