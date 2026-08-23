package com.chathala.hala.core.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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

/** سقف التراجع الأسّي بين المحاولات — خمس دقائق. */
private const val MAX_RETRY_MS = 5 * 60 * 1000L

/**
 * أقصى عدد خانات **خاملة** (غير معروضة) نحتفظ بها قبل التخلّص من الأقدم.
 *
 * يُقاس على الخاملة وحدها لا على مجموع الخانات: قائمة محادثات طويلة فيها بانر بعد
 * كل خمس محادثات تُنشئ خانات بعدد التمرير، فمقارنة المجموع كانت تُزيح خانة رآها
 * المستخدم قبل ثوانٍ، والعودة إليها تعني طلباً جديداً — و«لا امتلاء» يترك مكانه
 * فارغاً بعد أن كان فيه إعلان.
 */
private const val MAX_IDLE_SLOTS = 4

/**
 * مهلة حماية للخانة حديثة اللمس من الإزاحة.
 *
 * التركيب يقرأ حالة الخانة قبل أن يُشغّل `DisposableEffect` دالة الإرفاق، فتمرّ
 * لحظة تكون فيها الخانة موجودة و`active == 0`. بدون هذه المهلة تُزيحها أوّل خانة
 * أخرى تُرفَق، فيبقى العنصر مشتركاً في حالة خانة مهجورة ولا يظهر إعلانه أبداً.
 */
private const val EVICTION_GRACE_MS = 10_000L

/** صلاحية الإعلان المدمج لدى AdMob ساعة تقريباً — نجدّده قبلها بهامش. */
private const val NATIVE_TTL_MS = 45 * 60 * 1000L

private val mainHandler = Handler(Looper.getMainLooper())

/**
 * يؤجّل إلى الخيط الرئيسي — كل حالة المخزن تُقرأ وتُكتب هناك (التركيب + ردود AdMob)،
 * ونداءات التفريغ تأتي من [AdGate] على خيط خلفي.
 *
 * تأجيل دائم لا تنفيذ فوري عند وجودنا على الخيط الرئيسي أصلاً: التدمير يُستدعى من
 * نفس اللحظة التي تُخفي فيها الإعلان، فلا بدّ أن تكتمل إعادة التركيب أوّلاً وإلّا
 * بقي `NativeAdView` مربوطاً بإعلان مُدمَّر إطاراً كاملاً.
 */
private fun postMain(block: () -> Unit) {
    mainHandler.post(block)
}

// ─────────────────────────────── البانر ───────────────────────────────

/**
 * يحتفظ بـ `AdView` لكل خانة. الـ view نفسه يُعاد إرفاقه عند العودة، فيظهر الإعلان
 * فوراً بدل انتظار طلب جديد (وبدل ألّا يظهر أصلاً عند no fill).
 */
internal object BannerAdPool {

    private class Slot(val view: AdView, val widthDp: Int) {
        var loaded = false
        var requesting = false
        var lastFailAt = 0L
        /** إخفاقات متتالية — أساس التراجع الأسّي بين المحاولات. */
        var failures = 0
        var retryScheduled = false
        /** عدّاد لا راية: أثناء التنقّل قد تُرفَق الشاشة الجديدة قبل تحرير القديمة. */
        var active = 0
        var lastTouch = 0L
    }

    private val slots = LinkedHashMap<String, Slot>()

    /**
     * يُعيد `AdView` الخانة (يُنشئه ويطلب إعلاناً أول مرة)، جاهزاً للإرفاق.
     *
     * [widthDp] يأتي مقيساً من التخطيط ([BannerAd])، لا من `displayMetrics` — انظر
     * التعليل هناك.
     */
    fun obtain(context: Context, key: String, adUnitId: String, widthDp: Int): AdView {
        val now = System.currentTimeMillis()

        // مقاس البانر مثبَّت عند الإنشاء (`setAdSize` قبل التحميل)، والنشاط يُعاد
        // إنشاؤه عند الدوران بينما يبقى المخزون حيّاً — فبانر الوضع الرأسي يظهر
        // بعرض خاطئ في الأفقي. عند تغيّر العرض نبني بانراً بمقاس الاتجاه الجديد.
        slots[key]?.takeIf { it.widthDp != widthDp }?.let { stale ->
            AdLog.slot("«$key» تغيّر العرض ${stale.widthDp}→$widthDp، إعادة بناء (محمّل=${stale.loaded})")
            slots.remove(key)
            destroy(stale.view)
        }
        val existing = slots[key]
        if (existing != null) {
            AdLog.slot("«$key» إعادة استعمال — محمّل=${existing.loaded} جارٍ=${existing.requesting}")
        }
        val slot = existing ?: newSlot(context.applicationContext, key, adUnitId, widthDp)

        slot.active++
        slot.lastTouch = now
        // الـ view قد يكون ما يزال مُرفَقاً بالشاشة السابقة — الإضافة بأبٍ قائم تُسقط التطبيق.
        // لكن إن كان مرفَقاً وللخانة تركيب حيّ آخر، فالانتزاع يُفرِّغ بانراً ظاهراً:
        // مفتاح مكرَّر بين موضعين، وهو خطأ استدعاء يستحقّ أثراً في السجل.
        if (slot.view.parent != null && slot.active > 1) {
            Log.w(AdLog.TAG, "خانة البانر «$key» مطلوبة من تركيبين معاً — الـ view يُنتزع من الأول")
        }
        (slot.view.parent as? ViewGroup)?.removeView(slot.view)
        slot.view.resume()

        if (!slot.loaded && !slot.requesting && now - slot.lastFailAt >= retryDelay(slot)) {
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

    fun clearAll() = postMain {
        slots.values.forEach { destroy(it.view) }
        slots.clear()
    }

    private fun newSlot(appContext: Context, key: String, adUnitId: String, widthDp: Int): Slot {
        val view = AdView(appContext).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(appContext, widthDp))
            this.adUnitId = adUnitId
        }
        val slot = Slot(view, widthDp)
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                slot.loaded = true
                slot.requesting = false
                slot.failures = 0
                AdLog.loaded("بانر")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                slot.loaded = false
                slot.requesting = false
                slot.lastFailAt = System.currentTimeMillis()
                slot.failures++
                AdLog.failure("بانر", error)
                scheduleRetry(key, slot)
            }
        }
        slots[key] = slot
        AdLog.slot("«$key» خانة جديدة (المجموع=${slots.size})")
        return slot
    }

    private fun request(slot: Slot) {
        slot.requesting = true
        slot.view.loadAd(AdRequest.Builder().build())
    }

    /** تراجع أسّي: 30ث ثم 60 ثم 120… بسقف [MAX_RETRY_MS]. */
    private fun retryDelay(slot: Slot): Long =
        (RETRY_AFTER_MS shl (slot.failures - 1).coerceIn(0, 4)).coerceAtMost(MAX_RETRY_MS)

    /**
     * يعيد المحاولة لخانة **معروضة** فشل طلبها.
     *
     * بدون هذا يبقى الموضع فارغاً ما دام المستخدم واقفاً على الشاشة: `obtain` وحده
     * كان يطلق الطلبات، فلا شيء يعيد المحاولة حتى يخرج المستخدم ويعود. ومع نسبة
     * تعبئة منخفضة يعني ذلك أن أغلب المواضع تُقابَل برفض واحد ثم تُترك بيضاء.
     *
     * لا نعيد المحاولة لخانة غير معروضة: طلبٌ لا يراه أحد يُهدر ولا يُحتسب ظهوراً.
     */
    private fun scheduleRetry(key: String, slot: Slot) {
        if (slot.retryScheduled || slot.active == 0) return
        slot.retryScheduled = true
        mainHandler.postDelayed({
            slot.retryScheduled = false
            if (slots[key] !== slot) return@postDelayed
            if (slot.loaded || slot.requesting || slot.active == 0 || !AdGate.enabled) {
                return@postDelayed
            }
            AdLog.slot("«$key» إعادة محاولة (بعد ${slot.failures} إخفاق)")
            request(slot)
        }, retryDelay(slot))
    }

    /** يتخلّص من أقدم الخانات الخاملة فقط — المعروضة أو حديثة اللمس لا تُمسّ. */
    private fun trim() {
        val idle = slots.entries.filter { it.value.active == 0 }
        var excess = idle.size - MAX_IDLE_SLOTS
        if (excess <= 0) return
        val cutoff = System.currentTimeMillis() - EVICTION_GRACE_MS
        idle
            .filter { it.value.lastTouch < cutoff }
            .sortedBy { it.value.lastTouch }
            .forEach { entry ->
                if (excess <= 0) return
                AdLog.slot("«${entry.key}» إزاحة خانة خاملة (محمّل=${entry.value.loaded})")
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
    fun slotOf(key: String): Slot = slots.getOrPut(key) {
        // ختم الوقت عند الإنشاء: خانة بـ lastTouch = 0 تتصدّر ترتيب الإزاحة فتُحذف
        // قبل أن يلحقها الإرفاق أصلاً.
        Slot().apply { lastTouch = System.currentTimeMillis() }
    }

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

    fun clearAll() = postMain {
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
        val idle = slots.entries.filter { it.value.active == 0 }
        var excess = idle.size - MAX_IDLE_SLOTS
        if (excess <= 0) return
        val cutoff = System.currentTimeMillis() - EVICTION_GRACE_MS
        idle
            .filter { it.value.lastTouch < cutoff }
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
