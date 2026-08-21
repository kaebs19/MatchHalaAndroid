package com.chathala.hala.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * بانر AdMob متجاوب (Adaptive anchored) بعرض الشاشة.
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
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { BannerAdPool.obtain(context, slot, adUnitId) },
        onRelease = { BannerAdPool.release(slot) }
    )
}
