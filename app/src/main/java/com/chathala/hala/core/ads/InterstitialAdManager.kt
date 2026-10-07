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
 *  - فتح محادثة ([maybeShowOnChatOpen]).
 *  - الاكتشاف: بعد كل [AdConfig.DISCOVER_INTERSTITIAL_EVERY_CARDS] بطاقة ([showNow]).
 *
 * الموضعان يخضعان لسقف واحد: إعلان بيني واحد على الأكثر كل
 * [AdConfig.INTERSTITIAL_MIN_INTERVAL_MS]، محفوظ على القرص.
 */
object InterstitialAdManager {

    private var ad: InterstitialAd? = null
    private var loading = false
    private const val PREFS = "hala_ads"
    private const val KEY_LAST_SHOWN_AT = "interstitial_last_shown_at"

    /** يبدأ تحميل إعلان جاهز للعرض لاحقاً (آمن للاستدعاء المتكرر). */
    fun preload(context: Context) {
        if (!AdGate.enabled) return          // لا إعلانات للمشتركين
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
                    AdLog.loaded("بيني")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                    AdLog.failure("بيني", error)
                }
            }
        )
    }

    /** يعرض البيني في الاكتشاف إن كان جاهزاً ومرّ الفاصل الأدنى. يرجّع true لو عُرض. */
    fun showNow(activity: Activity): Boolean = showIfReady(activity)

    /** يعرض البيني عند فتح محادثة إن كان جاهزاً ومرّ الفاصل الأدنى. */
    fun maybeShowOnChatOpen(activity: Activity) {
        showIfReady(activity)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun showIfReady(activity: Activity): Boolean {
        if (!AdGate.enabled) return false    // لا إعلانات للمشتركين
        val now = System.currentTimeMillis()
        val lastShownAt = prefs(activity).getLong(KEY_LAST_SHOWN_AT, 0L)
        // `now < lastShownAt` = ساعة الجهاز رجعت للخلف؛ لا نحبس الإعلان للأبد بسببها
        if (now >= lastShownAt && now - lastShownAt < AdConfig.INTERSTITIAL_MIN_INTERVAL_MS) {
            preload(activity.applicationContext)
            return false
        }
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
        prefs(activity).edit().putLong(KEY_LAST_SHOWN_AT, now).apply()
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
