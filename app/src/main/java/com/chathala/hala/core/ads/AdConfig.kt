package com.chathala.hala.core.ads

import com.chathala.hala.BuildConfig

/**
 * إعدادات إعلانات AdMob — مكان واحد للمعرّفات وثوابت التكرار.
 *
 * مهم: في وضع debug نستخدم **معرّفات Google التجريبية** (إلزامي بسياسة AdMob —
 * النقر على إعلاناتك الحقيقية أثناء التطوير قد يوقف حسابك). الحقيقية في release فقط.
 */
object AdConfig {

    private val debug = BuildConfig.DEBUG

    // ── معرّفات Google التجريبية (لا تُغيّر) ──
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // ── المعرّفات الحقيقية ──
    private const val REAL_BANNER = "ca-app-pub-8219247197168750/7322228459"
    private const val REAL_INTERSTITIAL = "ca-app-pub-8219247197168750/4696065119"
    private const val REAL_NATIVE = "ca-app-pub-8219247197168750/5165848728"

    val bannerUnitId: String get() = if (debug) TEST_BANNER else REAL_BANNER
    val interstitialUnitId: String get() = if (debug) TEST_INTERSTITIAL else REAL_INTERSTITIAL
    val nativeUnitId: String get() = if (debug) TEST_NATIVE else REAL_NATIVE

    // ── ثوابت التكرار/المواضع ──
    /** بانر بعد كل N محادثة في قائمة المحادثات. */
    const val CHAT_LIST_BANNER_EVERY = 5

    /** إعلان بيني بعد كل N بطاقة في الاكتشاف. */
    const val DISCOVER_INTERSTITIAL_EVERY_CARDS = 15

    /** بطاقة إعلان مدمج بعد كل N بطاقة في مكدّس الاكتشاف. */
    const val NATIVE_EVERY_CARDS = 8

    /** إعلان مدمج بعد كل N نتيجة في قائمة البحث عن المستخدمين. */
    const val SEARCH_NATIVE_EVERY = 6

    /** أقل فاصل بين إعلانَي البيني عند فتح المحادثات: 20 دقيقة (تقليل الإزعاج). */
    const val CHAT_OPEN_INTERSTITIAL_INTERVAL_MS = 20 * 60 * 1000L

    /**
     * البيني يُعرض فقط إذا كان محمّلاً مسبقاً (لا ننتظر التحميل) — يضمن ألّا يُعطّل
     * المستخدم؛ ومهلة التحميل المسبق لا تتجاوز هذه القيمة (5 ثوانٍ).
     */
    const val INTERSTITIAL_LOAD_TIMEOUT_MS = 5_000L
}
