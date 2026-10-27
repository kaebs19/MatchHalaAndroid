package com.chathala.hala.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference

/** Ready-only fullscreen ads at discovery breaks, with a shared durable frequency cap. */
object InterstitialAdManager {
    private var ad: InterstitialAd? = null
    private var owner = WeakReference<Activity>(null)
    private var loading = false
    private var showing = false
    private var generation = 0
    private var loadedAt = 0L
    private var lastFailure = 0L
    private var foregroundStartedAt = System.currentTimeMillis()
    private const val PREFS = "hala_ads"
    private const val LAST_SHOWN = "interstitial_last_shown_at"

    fun onForegroundStarted() { foregroundStartedAt = System.currentTimeMillis() }

    fun preload(context: Context) {
        val activity = context.findActivity() ?: return
        if (!AdGate.enabled || !activity.canLoadAds() || showing || loading) return
        val now = System.currentTimeMillis()
        if (ad != null && owner.get() === activity && isFreshAd(loadedAt, now, AdConfig.INTERSTITIAL_TTL_MS)) return
        ad = null
        if (now - lastFailure < 30_000L) return
        owner = WeakReference(activity)
        loading = true
        val request = ++generation
        InterstitialAd.load(activity, AdConfig.interstitialUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(loaded: InterstitialAd) {
                if (generation != request || !AdGate.enabled || owner.get()?.isDestroyed != false) return
                ad = loaded; loading = false; loadedAt = System.currentTimeMillis()
                AdLog.loaded("بيني", loaded.responseInfo)
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                if (generation != request) return
                ad = null; loading = false; lastFailure = System.currentTimeMillis()
                AdLog.failure("بيني", error)
            }
        })
    }

    fun showNow(activity: Activity): Boolean {
        if (!AdGate.enabled || !activity.canLoadAds() || showing) return false
        val now = System.currentTimeMillis()
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!canShowInterstitial(now, foregroundStartedAt, prefs.getLong(LAST_SHOWN, 0L))) return false
        val current = ad
        if (current == null || owner.get() !== activity || !isFreshAd(loadedAt, now, AdConfig.INTERSTITIAL_TTL_MS)) {
            preload(activity)
            return false
        }
        ad = null; showing = true
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                prefs.edit().putLong(LAST_SHOWN, System.currentTimeMillis()).apply()
            }
            override fun onAdDismissedFullScreenContent() { showing = false; preload(activity) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { showing = false; lastFailure = System.currentTimeMillis() }
        }
        return try {
            current.show(activity)
            true
        } catch (error: RuntimeException) {
            showing = false
            lastFailure = System.currentTimeMillis()
            false
        }
    }

    fun clear() { adMainHandler.post { reset() } }
    internal fun clearForActivity(activity: Activity) { if (owner.get() === activity) reset() }
    private fun reset() { generation++; ad = null; loading = false; owner.clear() }
}

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
