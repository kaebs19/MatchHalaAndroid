package com.chathala.hala.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
    // `key(slot)` ليس تجميلاً: `factory` تُنفَّذ مرّة واحدة ولا تُعاد عند تغيّر
    // الوسائط. وقائمة المحادثات تشتقّ المفتاح من ترتيب العنصر بينما عناصرها
    // مُفتَّحة بمعرّف المحادثة — فوصول رسالة يُعيد ترتيب القائمة فيتبدّل [slot]
    // على تركيب قائم: يظل معروضاً view الخانة القديمة، ويبقى عدّادها `active`
    // مرفوعاً أبداً، ثم أوّل تركيب يطلب تلك الخانة بحقّ ينتزع الـ view من الشاشة
    // (`removeView`) فيختفي بانر ظاهر. الـ key يفرض تخلّصاً وإنشاءً نظيفين.
    key(slot) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { BannerAdPool.obtain(context, slot, adUnitId) },
            onRelease = { BannerAdPool.release(slot) }
        )
    }
}
