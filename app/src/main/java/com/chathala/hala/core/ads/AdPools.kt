package com.chathala.hala.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.lang.ref.WeakReference

private const val RETRY_AFTER_MS = 30_000L
private const val MAX_IDLE_SLOTS = 4
private const val EVICTION_GRACE_MS = 10_000L
private const val NATIVE_TTL_MS = 45 * 60 * 1000L
internal val adMainHandler = Handler(Looper.getMainLooper())

internal fun Activity.canLoadAds(): Boolean = !isFinishing && !isDestroyed &&
    ((this as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false)

internal fun releaseActivityAds(activity: Activity) {
    BannerAdPool.clearForActivity(activity)
    NativeAdPool.clearForActivity(activity)
    InterstitialAdManager.clearForActivity(activity)
}

/** Banners retain their view while scrolling, but never cross Activity or size boundaries. */
internal object BannerAdPool {
    internal class Slot(val view: AdView, val width: Int, val unitId: String, owner: Activity) {
        val owner = WeakReference(owner)
        var loaded by mutableStateOf(false)
        var requesting by mutableStateOf(false)
        var lastFailure = 0L
        var failures = 0
        var active = 0
        var lastTouch = System.currentTimeMillis()
    }
    private val slots = mutableStateMapOf<String, Slot>()
    fun stateOf(key: String): Slot? = slots[key]

    fun obtain(context: Context, key: String, unitId: String, width: Int): AdView {
        val activity = requireNotNull(context.findActivity())
        slots[key]?.takeIf { it.width != width || it.unitId != unitId || it.owner.get() !== activity }?.let {
            slots.remove(key)
            destroy(it)
        }
        val slot = slots[key] ?: create(activity, key, unitId, width).also { slots[key] = it }
        slot.active++
        slot.lastTouch = System.currentTimeMillis()
        (slot.view.parent as? ViewGroup)?.removeView(slot.view)
        resume(key)
        trim()
        return slot.view
    }

    fun release(key: String, view: AdView) {
        val slot = slots[key]?.takeIf { it.view === view } ?: return
        slot.active = (slot.active - 1).coerceAtLeast(0)
        slot.lastTouch = System.currentTimeMillis()
        if (slot.active == 0) slot.view.pause()
    }

    fun resume(key: String) {
        val slot = slots[key] ?: return
        if (!AdGate.enabled || slot.active == 0 || slot.owner.get()?.canLoadAds() != true) return
        slot.view.resume()
        if (!slot.loaded && !slot.requesting && System.currentTimeMillis() - slot.lastFailure >= retryDelay(slot)) {
            slot.requesting = true
            slot.view.loadAd(AdRequest.Builder().build())
        }
    }
    fun pause(key: String) { slots[key]?.view?.pause() }
    fun clearAll() { adMainHandler.post { slots.values.toList().forEach(::destroy); slots.clear() } }
    fun clearForActivity(activity: Activity) {
        slots.entries.filter { it.value.owner.get() === activity }.map { it.key }.forEach { key -> slots.remove(key)?.let(::destroy) }
    }

    private fun create(activity: Activity, key: String, unitId: String, width: Int): Slot {
        val view = AdView(activity).apply {
            adUnitId = unitId
            setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, width))
        }
        val slot = Slot(view, width, unitId, activity)
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (slots[key] !== slot || !AdGate.enabled) return
                slot.loaded = true
                slot.failures = 0
                slot.requesting = false
                AdLog.loaded("بانر", view.responseInfo)
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                if (slots[key] !== slot) return
                slot.requesting = false
                slot.lastFailure = System.currentTimeMillis()
                AdLog.failure("بانر", error)
                if (!slot.loaded) {
                    slot.failures++
                    // A collapsed unfilled banner is not visible, so SDK auto-refresh may be paused.
                    val failure = slot.lastFailure
                    adMainHandler.postDelayed({
                        if (slots[key] === slot && slot.lastFailure == failure && !slot.loaded) resume(key)
                    }, retryDelay(slot))
                }
                // Once a creative is visible, the unit's SDK auto-refresh owns its refreshes.
            }
        }
        return slot
    }

    private fun retryDelay(slot: Slot): Long =
        (RETRY_AFTER_MS shl (slot.failures - 1).coerceIn(0, 4)).coerceAtMost(300_000L)

    private fun trim() {
        val idle = slots.entries.filter { it.value.active == 0 }.sortedBy { it.value.lastTouch }
        idle.take((idle.size - MAX_IDLE_SLOTS).coerceAtLeast(0)).filter {
            System.currentTimeMillis() - it.value.lastTouch > EVICTION_GRACE_MS
        }.forEach { slots.remove(it.key)?.let(::destroy) }
    }
    private fun destroy(slot: Slot) {
        (slot.view.parent as? ViewGroup)?.removeView(slot.view)
        slot.view.destroy()
    }
}

/** One native object per placement, Activity-bound loads, bounded caching and stale-response guards. */
internal object NativeAdPool {
    internal class Slot {
        var ad by mutableStateOf<NativeAd?>(null)
        var loading by mutableStateOf(false)
        var loadedAt = 0L
        var lastFailure = 0L
        var failures = 0
        var active = 0
        var generation = 0
        var lastTouch = System.currentTimeMillis()
        var owner = WeakReference<Activity>(null)
    }
    private val slots = LinkedHashMap<String, Slot>()
    fun slotOf(key: String): Slot = slots.getOrPut(key) { Slot() }

    fun attach(context: Context, key: String) {
        val activity = context.findActivity() ?: return
        val slot = slotOf(key)
        if (slot.owner.get() !== activity) {
            slot.generation++
            slot.ad?.destroy()
            slot.ad = null
            slot.loading = false
            slot.active = 0
            slot.lastFailure = 0L
            slot.owner = WeakReference(activity)
        }
        slot.active++
        slot.lastTouch = System.currentTimeMillis()
        if (slot.ad != null && !isFreshAd(slot.loadedAt, System.currentTimeMillis(), NATIVE_TTL_MS)) {
            slot.ad?.destroy(); slot.ad = null
        }
        requestIfEligible(key, slot)
        trim()
    }

    fun detach(key: String, activity: Activity) {
        val slot = slots[key]?.takeIf { it.owner.get() === activity } ?: return
        slot.active = (slot.active - 1).coerceAtLeast(0)
        slot.lastTouch = System.currentTimeMillis()
    }

    fun invalidate(key: String) { adMainHandler.post {
        val slot = slots[key] ?: return@post
        slot.generation++
        slot.ad?.destroy(); slot.ad = null
        slot.loading = false; slot.loadedAt = 0L; slot.lastFailure = 0L
        requestIfEligible(key, slot)
    } }
    fun clearAll() { adMainHandler.post { slots.values.forEach { it.generation++; it.ad?.destroy(); it.ad = null }; slots.clear() } }
    fun clearForActivity(activity: Activity) {
        slots.entries.filter { it.value.owner.get() === activity }.map { it.key }.forEach { key ->
            slots.remove(key)?.let { it.generation++; it.ad?.destroy(); it.ad = null }
        }
    }

    private fun requestIfEligible(key: String, slot: Slot) {
        val activity = slot.owner.get() ?: return
        if (!AdGate.enabled || slot.active == 0 || !activity.canLoadAds() || slot.ad != null || slot.loading) return
        val delay = (RETRY_AFTER_MS shl (slot.failures - 1).coerceIn(0, 4)).coerceAtMost(300_000L)
        if (System.currentTimeMillis() - slot.lastFailure < delay) return
        slot.loading = true
        val generation = ++slot.generation
        AdLoader.Builder(activity, AdConfig.nativeUnitId)
            .withNativeAdOptions(NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE).build())
            .forNativeAd { loaded ->
                if (slots[key] !== slot || slot.generation != generation || !AdGate.enabled || slot.owner.get()?.isDestroyed != false) {
                    loaded.destroy()
                } else {
                    slot.loading = false; slot.ad = loaded
                    slot.loadedAt = System.currentTimeMillis(); slot.failures = 0
                    AdLog.loaded("مدمج", loaded.responseInfo)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (slots[key] !== slot || slot.generation != generation) return
                    slot.loading = false; slot.lastFailure = System.currentTimeMillis(); slot.failures++
                    AdLog.failure("مدمج", error)
                    val retry = (RETRY_AFTER_MS shl (slot.failures - 1).coerceIn(0, 4)).coerceAtMost(300_000L)
                    adMainHandler.postDelayed({ if (slots[key] === slot && slot.generation == generation) requestIfEligible(key, slot) }, retry)
                }
            }).build().loadAd(AdRequest.Builder().build())
    }

    private fun trim() {
        val idle = slots.entries.filter { it.value.active == 0 }.sortedBy { it.value.lastTouch }
        idle.take((idle.size - MAX_IDLE_SLOTS).coerceAtLeast(0)).filter {
            System.currentTimeMillis() - it.value.lastTouch > EVICTION_GRACE_MS
        }.forEach { slots.remove(it.key)?.ad?.destroy() }
    }
}
