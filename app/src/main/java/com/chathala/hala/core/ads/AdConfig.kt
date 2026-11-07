package com.chathala.hala.core.ads

import com.chathala.hala.BuildConfig

/**
 * إعدادات إعلانات AdMob — مكان واحد للمعرّفات وثوابت التكرار.
 *
 * مهم: في وضع debug نستخدم **معرّفات Google التجريبية** (إلزامي بسياسة AdMob —
 * النقر على إعلاناتك الحقيقية أثناء التطوير قد يوقف حسابك). الحقيقية في release فقط.
 */
object AdConfig {

    private val debug = BuildConfig.DEBUG && !BuildConfig.MEDIATION_TESTING

    // ── معرّفات Google التجريبية (لا تُغيّر) ──
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // ── المعرّفات الحقيقية ──
    private const val REAL_BANNER = "ca-app-pub-8219247197168750/4061232146"
    private const val REAL_INTERSTITIAL = "ca-app-pub-8219247197168750/1547729387"
    private const val REAL_NATIVE = "ca-app-pub-8219247197168750/5625595255"

    val bannerUnitId: String get() = if (debug) TEST_BANNER else REAL_BANNER
    val interstitialUnitId: String get() = if (debug) TEST_INTERSTITIAL else REAL_INTERSTITIAL
    val nativeUnitId: String get() = if (debug) TEST_NATIVE else REAL_NATIVE

    // ── ثوابت التكرار/المواضع ──
    /** بانر بعد كل N محادثة في قائمة المحادثات. */
    const val CHAT_LIST_BANNER_EVERY = 5

    /**
     * محطة إعلان كل N بطاقة في الاكتشاف، بالتناوب: مدمج ← ملء الشاشة ← مدمج…
     * (مطابق لـ iOS 11.6). إن تعذّر ملء الشاشة يظهر المدمج بدله.
     */
    const val DISCOVER_AD_BREAK_EVERY = 6

    /** إعلان مدمج بعد كل N نتيجة في قائمة البحث عن المستخدمين. */
    const val SEARCH_NATIVE_EVERY = 6

    /**
     * أقل فاصل بين أي إعلانَين بينيّين — مشترك بين فتح المحادثات والاكتشاف، ومحفوظ
     * على القرص فلا يُصفّره إغلاق التطبيق وفتحه: 45 دقيقة (مطابق لـ iOS).
     */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 45 * 60 * 1000L

    /** سقف الإعلانات البينية في اليوم (بتوقيت الجهاز) — مطابق لـ iOS. */
    const val INTERSTITIAL_DAILY_CAP = 5

    /** Discard cached fullscreen ads before their one-hour SDK expiry. */
    const val INTERSTITIAL_TTL_MS = 45 * 60 * 1000L

    /** إعلان مدمج بين القصص: «متابعة القصص» يظهر بعد هذه المهلة (أقل من حدّ الحبس 8ث). */
    const val STORY_AD_READ_DELAY_MS = 3_000L

    /** Never interrupt the first minute of a foreground discovery session. */
    const val INTERSTITIAL_WARMUP_MS = 60_000L
}
