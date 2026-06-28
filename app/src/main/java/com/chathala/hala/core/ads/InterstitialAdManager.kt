package com.chathala.hala.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * يدير الإعلان البيني (Interstitial): يحمّله مسبقاً ويعرضه **فقط إذا كان جاهزاً** —
 * لا ننتظر التحميل أبداً حتى لا نُعطّل المستخدم (يحقّق شرط ألّا يتجاوز التأخير 5 ثوانٍ).
 *
 * مَواضع العرض:
 *  - فتح محادثة: مرة كل نصف ساعة كحدّ أقصى ([maybeShowOnChatOpen]).
 *  - الاكتشاف: بعد كل 10 بطاقات ([showNow]).
 */
object InterstitialAdManager {

    private var ad: InterstitialAd? = null
    private var loading = false
    private var lastChatOpenShownAt = 0L

    /** يبدأ تحميل إعلان جاهز للعرض لاحقاً (آمن للاستدعاء المتكرر). */
    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true
        val appCtx = context.applicationContext
        InterstitialAd.load(
            appCtx,
            AdConfig.interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                }
            }
        )
    }

    /** يعرض البيني فوراً إن كان جاهزاً. يرجّع true لو عُرض. */
    fun showNow(activity: Activity): Boolean = showIfReady(activity)

    /** يعرض البيني عند فتح محادثة بشرط مرور نصف ساعة على آخر عرض. */
    fun maybeShowOnChatOpen(activity: Activity) {
        val now = System.currentTimeMillis()
        if (now - lastChatOpenShownAt < AdConfig.CHAT_OPEN_INTERSTITIAL_INTERVAL_MS) {
            preload(activity.applicationContext)
            return
        }
        if (showIfReady(activity)) lastChatOpenShownAt = now
    }

    private fun showIfReady(activity: Activity): Boolean {
        val current = ad
        if (current == null) {
            preload(activity.applicationContext)
            return false
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad = null
                preload(activity.applicationContext)
            }
        }
        ad = null
        current.show(activity)
        return true
    }
}

/** يستخرج Activity من Context (يفكّ ContextWrapper) — لعرض الإعلانات من Compose. */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
