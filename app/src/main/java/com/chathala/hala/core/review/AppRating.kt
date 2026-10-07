package com.chathala.hala.core.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * متى نطلب من المستخدم تقييم التطبيق، وكيف.
 *
 * المبدأ: لا نسأل إلا مستخدماً جرّب التطبيق فعلاً، وفي لحظة هادئة، ونادراً.
 *  - مرّت [MIN_DAYS_SINCE_INSTALL] أيام على أول تشغيل، و[MIN_SESSIONS] جلسات على الأقل.
 *  - حصلت له [MIN_POSITIVE_EVENTS] لحظات إيجابية على الأقل (تطابق، رسالة أرسلها).
 *  - لا يُعرض إلا عند **العودة** للشاشة الرئيسية (من محادثة مثلاً)، لا عند فتح التطبيق
 *    — فلا يقطع شيئاً ولا يتزاحم مع طلب إذن الإشعارات.
 *  - مرة واحدة في الجلسة، و«ليس الآن» تؤجّل [REASK_AFTER_LATER_DAYS] يوماً، وبعد
 *    [MAX_LATER_COUNT] تأجيلات نتوقّف. «قيّم» و«لا تسألني» تُنهيان الطلب نهائياً.
 *
 * التقييم نفسه عبر Play In-App Review (داخل التطبيق دون مغادرته). Google تحدّ من
 * ظهوره بحصّة لا نعرفها، فإن أُغلق التدفّق فوراً (لم يظهر شيء) نفتح صفحة المتجر —
 * المستخدم ضغط «قيّم» بنفسه، فلا يصحّ ألا يحدث شيء.
 */
object AppRating {

    private const val PREFS = "hala_rating"
    private const val KEY_FIRST_SEEN_AT = "first_seen_at"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_POSITIVE = "positive_events"
    private const val KEY_LAST_PROMPT_AT = "last_prompt_at"
    private const val KEY_LATER_COUNT = "later_count"
    private const val KEY_DONE = "done"

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MIN_DAYS_SINCE_INSTALL = 3
    private const val MIN_SESSIONS = 4
    private const val MIN_POSITIVE_EVENTS = 3
    private const val REASK_AFTER_LATER_DAYS = 14
    private const val MAX_LATER_COUNT = 3

    /** أقل من هذا بين الإطلاق والإغلاق = لم تظهر نافذة التقييم (استُنفدت الحصّة). */
    private const val FLOW_SHOWN_THRESHOLD_MS = 1_000L

    private var sessionCounted = false
    private var promptedThisSession = false
    private var mainEntries = 0

    private lateinit var appContext: Context

    /** يُستدعى مرة من [com.chathala.hala.HalaApp.onCreate]. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** يُستدعى عند كل دخول للشاشة الرئيسية؛ يعدّ الجلسة مرة واحدة لكل تشغيل. */
    fun onMainScreenEntered() {
        mainEntries++
        if (sessionCounted) return
        sessionCounted = true
        val p = prefs()
        p.edit()
            .apply { if (!p.contains(KEY_FIRST_SEEN_AT)) putLong(KEY_FIRST_SEEN_AT, System.currentTimeMillis()) }
            .putInt(KEY_SESSIONS, p.getInt(KEY_SESSIONS, 0) + 1)
            .apply()
    }

    /** لحظة إيجابية للمستخدم: تطابق، أو رسالة أُرسلت بنجاح. */
    fun notePositiveEvent() {
        val p = prefs()
        if (p.getBoolean(KEY_DONE, false)) return
        p.edit().putInt(KEY_POSITIVE, p.getInt(KEY_POSITIVE, 0) + 1).apply()
    }

    /** هل نعرض الطلب الآن؟ */
    fun shouldPrompt(): Boolean {
        if (promptedThisSession) return false
        if (mainEntries < 2) return false    // الدخول الأول = فتح التطبيق، لا عودة
        val p = prefs()
        if (p.getBoolean(KEY_DONE, false)) return false
        if (p.getInt(KEY_LATER_COUNT, 0) >= MAX_LATER_COUNT) return false
        val now = System.currentTimeMillis()
        val firstSeen = p.getLong(KEY_FIRST_SEEN_AT, now)
        if (now - firstSeen < MIN_DAYS_SINCE_INSTALL * DAY_MS) return false
        if (p.getInt(KEY_SESSIONS, 0) < MIN_SESSIONS) return false
        if (p.getInt(KEY_POSITIVE, 0) < MIN_POSITIVE_EVENTS) return false
        val lastPrompt = p.getLong(KEY_LAST_PROMPT_AT, 0L)
        return now - lastPrompt >= REASK_AFTER_LATER_DAYS * DAY_MS
    }

    /** النافذة ظهرت — لا تتكرّر في هذه الجلسة. */
    fun markPrompted() {
        promptedThisSession = true
        prefs().edit().putLong(KEY_LAST_PROMPT_AT, System.currentTimeMillis()).apply()
    }

    fun onLater() {
        val p = prefs()
        p.edit().putInt(KEY_LATER_COUNT, p.getInt(KEY_LATER_COUNT, 0) + 1).apply()
    }

    fun onNever() {
        prefs().edit().putBoolean(KEY_DONE, true).apply()
    }

    /** يطلق تقييم Play داخل التطبيق، ويفتح صفحة المتجر إن لم يظهر. */
    fun launchReview(activity: Activity) {
        prefs().edit().putBoolean(KEY_DONE, true).apply()
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) {
                openStoreListing(activity)
                return@addOnCompleteListener
            }
            val startedAt = SystemClock.elapsedRealtime()
            manager.launchReviewFlow(activity, request.result).addOnCompleteListener {
                if (SystemClock.elapsedRealtime() - startedAt < FLOW_SHOWN_THRESHOLD_MS) {
                    openStoreListing(activity)
                }
            }
        }
    }

    private fun openStoreListing(activity: Activity) {
        val pkg = activity.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            .setPackage("com.android.vending")
        runCatching { activity.startActivity(market) }.onFailure {
            runCatching {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                )
            }
        }
    }
}
