package com.chathala.hala.core.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chathala.hala.core.storage.AppLanguage
import java.util.Locale

/**
 * مصدر الحقيقة الوحيد للغة الواجهة أثناء التشغيل.
 *
 * يحتفظ بـ Context مُترجَم (localized) يُبنى مرّة واحدة لكل لغة، ويُستخدم من:
 *  - الـ Composables عبر [S] — والتبديل الفوري مضمون لأن [current] حالة Compose
 *    ويُعاد تركيب شجرة الواجهة كاملة عبر `key(language)` في MainActivity.
 *  - الـ ViewModels والـ Repositories عبر [S] أيضاً — لا تحتاج Context.
 *
 * يجب استدعاء [init] في `HalaApp.onCreate` قبل أي قراءة.
 */
object LocaleManager {

    private lateinit var appContext: Context
    private val contexts = mutableMapOf<AppLanguage, Context>()

    /** حالة Compose — تغييرها يُعيد تركيب كل ما يقرأ نصوصاً. */
    var current by mutableStateOf(AppLanguage.ARABIC)
        private set

    val isRtl: Boolean get() = current == AppLanguage.ARABIC

    /** الـ Locale الحالي — لمنسّقات التاريخ والأرقام (SimpleDateFormat وغيرها). */
    val locale: Locale get() = Locale.forLanguageTag(current.tag)

    fun init(context: Context, language: AppLanguage) {
        appContext = context.applicationContext
        current = language
    }

    fun setLanguage(language: AppLanguage) {
        if (::appContext.isInitialized) current = language
    }

    /** Context مضبوط على اللغة المطلوبة — مُخزَّن مؤقتاً لتفادي إعادة البناء. */
    fun contextFor(language: AppLanguage): Context = contexts.getOrPut(language) {
        val locale = Locale.forLanguageTag(language.tag)
        // لا نستدعي Locale.setDefault: تغيير الافتراضي عالمياً كان سيجعل
        // AppLanguage.fromDevice() (كشف لغة الجهاز عند أول تشغيل) يقرأ اختيارنا
        // لا لغة الجهاز. المنسّقات تأخذ اللغة صراحةً من LocaleManager.locale.
        val config = Configuration(appContext.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        appContext.createConfigurationContext(config)
    }

    /** Context اللغة الحالية. */
    val context: Context get() = contextFor(current)
}

/**
 * الوصول المُوحّد للنصوص — يعمل داخل Composables و ViewModels و Repositories.
 *
 * ```
 * Text(S.get(R.string.btn_login))
 * Text(S.get(R.string.greeting, userName))
 * ```
 */
object S {

    fun get(resId: Int): String = LocaleManager.context.getString(resId)

    fun get(resId: Int, vararg args: Any?): String =
        LocaleManager.context.getString(resId, *args)

    fun plural(resId: Int, quantity: Int): String =
        LocaleManager.context.resources.getQuantityString(resId, quantity, quantity)

    /** يختار النص حسب اللغة الحالية — للحالات التي لا تناسبها موارد النظام (بيانات مثل أسماء الدول). */
    fun pick(ar: String, en: String): String = if (LocaleManager.isRtl) ar else en

    private val ARABIC_SCRIPT = Regex("[\\u0600-\\u06FF]")

    /**
     * نصٌّ وارد من الخادم مع بديل محلي مُترجَم.
     *
     * الخادم يردّ بالعربية دائماً (لا يتفاوض على اللغة)، فتفضيل رسالته حرفياً
     * كان يُسرّب العربية إلى الواجهة الإنجليزية. هنا نتجاهلها إن كانت عربية
     * والواجهة إنجليزية: رسالة عامة مترجَمة أوضح للمستخدم من نصّ لا يقرأه.
     *
     * تبقى رسالة الخادم مُفضَّلة في الوضع العربي لأنها أدقّ وأحدث من البديل.
     */
    fun serverOr(serverMessage: String?, fallbackRes: Int): String {
        val msg = serverMessage?.trim()
        if (msg.isNullOrBlank()) return get(fallbackRes)
        if (!LocaleManager.isRtl && ARABIC_SCRIPT.containsMatchIn(msg)) return get(fallbackRes)
        return msg
    }
}
