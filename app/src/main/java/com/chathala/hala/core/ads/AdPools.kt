package com.chathala.hala.core.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

/**
 * مخزن الإعلانات — يبقى الإعلان حيّاً بعد خروج الواجهة من التركيب.
 *
 * لماذا؟ كان كل موضع إعلان ينشئ `AdView`/`NativeAd` جديداً في `factory` ويُدمّره في
 * `onDispose`. داخل `LazyColumn`/`LazyVerticalGrid` هذا يحدث مع كل تمرير يُخرج العنصر
 * من نافذة العرض، وعند كل مغادرة للشاشة وعودة. النتيجة: طلب AdMob جديد في كل مرة،
 * والطلبات المتلاحقة على نفس الوحدة تُقابَل بـ `no fill` (code 3) — فيختفي الإعلان
 * الذي كان ظاهراً قبل قليل.
 *
 * الحل: خانات (slots) بمفاتيح ثابتة؛ كل خانة تحتفظ بإعلانها بين مرّات الظهور،
 * وتُعيد الطلب فقط إذا لم يكن لديها إعلان صالح.
 *
 * كل الدوال تُستدعى من الخيط الرئيسي (Compose + ردود AdMob)، عدا [clearAll] التي
 * تُحوِّل نفسها إليه.
 */

/** أقل فاصل بين محاولتَي تحميل لخانة فشلت — لا نُغرق AdMob بطلبات فاشلة. */
private const val RETRY_AFTER_MS = 30_000L

/** أقصى عدد خانات خاملة (غير معروضة) نحتفظ بها قبل التخلّص من الأقدم. */
private const val MAX_IDLE_SLOTS = 4

/** صلاحية الإعلان المدمج لدى AdMob ساعة تقريباً — نجدّده قبلها بهامش. */
private const val NATIVE_TTL_MS = 45 * 60 * 1000L

private val mainHandler = Handler(Looper.getMainLooper())

private fun onMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
}

/** يؤجّل إلى الدورة التالية للخيط الرئيسي — لتغييرٍ يجب ألّا يسبق استقرار التركيب. */
private fun postMain(block: () -> Unit) {
    mainHandler.post(block)
}

// ─────────────────────────────── البانر ───────────────────────────────

/**
 * يحتفظ بـ `AdView` لكل خانة. الـ view نفسه يُعاد إرفاقه عند العودة، فيظهر الإعلان
 * فوراً بدل انتظار طلب جديد (وبدل ألّا يظهر أصلاً عند no fill).
 */
internal object BannerAdPool {

    private class Slot(val view: AdView) {
        var loaded = false
        var requesting = false
        var lastFailAt = 0L
        /** عدّاد لا راية: أثناء التنقّل قد تُرفَق الشاشة الجديدة قبل تحرير القديمة. */
        var active = 0
        var lastTouch = 0L
    }

    private val slots = LinkedHashMap<String, Slot>()

    /** يُعيد `AdView` الخانة (يُنشئه ويطلب إعلاناً أول مرة)، جاهزاً للإرفاق. */
    fun obtain(context: Context, key: String, adUnitId: String): AdView {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val slot = slots[key] ?: newSlot(appContext, key, adUnitId)

        slot.active++
        slot.lastTouch = now
        // الـ view قد يكون ما يزال مُرفَقاً بالشاشة السابقة — الإضافة بأبٍ قائم تُسقط التطبيق.
        (slot.view.parent as? ViewGroup)?.removeView(slot.view)
        slot.view.resume()

        if (!slot.loaded && !slot.requesting && now - slot.lastFailAt >= RETRY_AFTER_MS) {
            request(slot)
        }
        trim()
        return slot.view
    }

    /** الخانة لم تعد معروضة — لا تدمير، فقط إيقاف التحديث التلقائي. */
    fun release(key: String) {
        val slot = slots[key] ?: return
        slot.active = (slot.active - 1).coerceAtLeast(0)
        slot.lastTouch = System.currentTimeMillis()
        if (slot.active == 0) slot.view.pause()
    }

    fun clearAll() = onMain {
        slots.values.forEach { destroy(it.view) }
        slots.clear()
    }

    private fun newSlot(appContext: Context, key: String, adUnitId: String): Slot {
        val view = AdView(appContext).apply {
            setAdSize(adaptiveBannerSize(appContext))
            this.adUnitId = adUnitId
        }
        val slot = Slot(view)
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                slot.loaded = true
                slot.requesting = false
                AdLog.loaded("بانر")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                slot.loaded = false
                slot.requesting = false
                slot.lastFailAt = System.currentTimeMillis()
                AdLog.failure("بانر", error)
            }
        }
        slots[key] = slot
        return slot
    }

    private fun request(slot: Slot) {
        slot.requesting = true
        slot.view.loadAd(AdRequest.Builder().build())
    }

    /** يتخلّص من أقدم الخانات الخاملة فقط — المعروضة لا تُمسّ. */
    private fun trim() {
        var excess = slots.size - MAX_IDLE_SLOTS
        if (excess <= 0) return
        slots.entries
            .filter { it.value.active == 0 }
            .sortedBy { it.value.lastTouch }
            .forEach { entry ->
                if (excess <= 0) return
                slots.remove(entry.key)
                destroy(entry.value.view)
                excess--
            }
    }

    private fun destroy(view: AdView) {
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    }
}

internal fun adaptiveBannerSize(context: Context): AdSize {
    val metrics = context.resources.displayMetrics
    val widthDp = (metrics.widthPixels / metrics.density).toInt().coerceAtLeast(320)
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
}

// ─────────────────────────────── المدمج ───────────────────────────────

/**
 * يحتفظ بـ `NativeAd` لكل خانة. الإعلان المدمج لا يجوز ربطه بأكثر من `NativeAdView`
 * في وقت واحد (يفسد تتبّع الظهور لدى AdMob)، لذا لكل خانة إعلانها الخاص بدل مشاركة
 * إعلان واحد بين المواضع.
 */
internal object NativeAdPool {

    internal class Slot {
        /** حالة Compose: قراءتها من دالة @Composable تُعيد التركيب عند وصول الإعلان. */
        var ad by mutableStateOf<NativeAd?>(null)
        var loading by mutableStateOf(false)
        var loadedAt = 0L
        var lastFailAt = 0L
        var active = 0
        var lastTouch = 0L
    }

    private val slots = LinkedHashMap<String, Slot>()

    /**
     * يُعيد خانة المفتاح (يُنشئها فارغة إن لزم) **دون** بدء أي طلب.
     * الإنشاء هنا ضروري ليشترك التركيب في حالة `ad` قبل اكتمال التحميل.
     */
    fun slotOf(key: String): Slot = slots.getOrPut(key) { Slot() }

    /** الخانة صارت معروضة: يبدأ التحميل إن لم يكن لديها إعلان صالح. */
    fun attach(context: Context, key: String) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val slot = slotOf(key)
        slot.active++
        slot.lastTouch = now

        val current = slot.ad
        if (current != null && now - slot.loadedAt > NATIVE_TTL_MS) {
            slot.ad = null
            current.destroy()
        }
        if (slot.ad == null && !slot.loading && now - slot.lastFailAt >= RETRY_AFTER_MS) {
            request(appContext, key, slot)
        }
        trim()
    }

    fun detach(key: String) {
        val slot = slots[key] ?: return
        slot.active = (slot.active - 1).coerceAtLeast(0)
        slot.lastTouch = System.currentTimeMillis()
    }

    /**
     * يُسقِط إعلان الخانة ليُطلب غيره — لموضع يُعرض مراراً ويجب ألّا يكرّر نفس الإعلان.
     *
     * مؤجَّل عمداً: يُستدعى عادةً من نفس النقرة التي تُخفي الإعلان، والتدمير الفوري
     * يطال إعلاناً ما يزال مربوطاً بـ `NativeAdView` حتى تكتمل إعادة التركيب.
     */
    fun invalidate(key: String) = postMain {
        val slot = slots[key] ?: return@postMain
        slot.ad?.destroy()
        slot.ad = null
        slot.loadedAt = 0L
        slot.lastFailAt = 0L
    }

    fun clearAll() = onMain {
        slots.values.forEach { it.ad?.destroy(); it.ad = null }
        slots.clear()
    }

    private fun request(appContext: Context, key: String, slot: Slot) {
        if (!AdGate.enabled) return
        slot.loading = true
        AdLoader.Builder(appContext, AdConfig.nativeUnitId)
            .forNativeAd { loaded ->
                slot.loading = false
                // وصل بعد التخلّص من الخانة (تمرير سريع/خروج) — لا تسرّبه.
                if (slots[key] !== slot || !AdGate.enabled) {
                    loaded.destroy()
                    return@forNativeAd
                }
                slot.ad?.destroy()
                slot.ad = loaded
                slot.loadedAt = System.currentTimeMillis()
                AdLog.loaded("مدمج")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    slot.loading = false
                    slot.lastFailAt = System.currentTimeMillis()
                    AdLog.failure("مدمج", error)
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun trim() {
        var excess = slots.size - MAX_IDLE_SLOTS
        if (excess <= 0) return
        slots.entries
            .filter { it.value.active == 0 }
            .sortedBy { it.value.lastTouch }
            .forEach { entry ->
                if (excess <= 0) return
                slots.remove(entry.key)
                entry.value.ad?.destroy()
                entry.value.ad = null
                excess--
            }
    }
}
