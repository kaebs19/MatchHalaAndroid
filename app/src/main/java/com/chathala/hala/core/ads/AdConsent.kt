package com.chathala.hala.core.ads

import android.app.Activity
import com.chathala.hala.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * موافقة الخصوصية (UMP / نموذج موافقة Google المعتمد).
 *
 * بدون CMP معتمد، لا يُقدّم خادم الإعلانات إعلاناً لمستخدمي المنطقة الاقتصادية
 * الأوروبية وبريطانيا إطلاقاً (يردّ **No fill**)، ويفقد الطلب إشارات التخصيص في
 * بقية المناطق فتنخفض نسبة التعبئة. النموذج يُنشأ ويُدار من AdMob →
 * Privacy & messaging، وهذا الكود يعرضه عند اللزوم فقط.
 *
 * يُستدعى مرة واحدة من [com.chathala.hala.MainActivity]. آمن للتكرار.
 *
 * هو أيضاً مَن يُطلق [MobileAdsInitializer] — لا تُهيَّأ الإعلانات قبل حسم الموافقة.
 */
object AdConsent {

    @Volatile
    private var requested = false

    /**
     * هل يوجب النموذج إتاحة «خيارات الخصوصية» للمستخدم لاحقاً؟
     *
     * في أوروبا يُلزم Google بأن يجد المستخدم مدخلاً دائماً يُعيد فتح النموذج بعد
     * أوّل اختيار — وغيابه سبب رفض في المراجعة. تُقرأ من شاشة الخصوصية فيظهر المدخل
     * حيث يلزم فقط، ويختفي حيث لا يعني شيئاً.
     */
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun gather(activity: Activity) {
        if (requested) return
        requested = true

        // في debug نتظاهر بأننا داخل المنطقة الاقتصادية الأوروبية، وإلا فالنموذج
        // لا يظهر أصلاً خارجها فيستحيل اختباره. (المحاكي جهاز اختبار تلقائياً.)
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .build()
                )
            }
        }.build()

        val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        AdLog.consent("تعذّر عرض نموذج الموافقة: ${formError.message}")
                    }
                    AdLog.consent("canRequestAds=${info.canRequestAds()}")
                    syncState(activity, info)
                }
            },
            { requestError ->
                AdLog.consent("فشل تحديث حالة الموافقة: ${requestError.message}")
                // فشل التحديث لا يُلغي موافقة محفوظة من جلسة سابقة.
                syncState(activity, info)
            }
        )

        // موافقة محسومة في جلسة سابقة: لا داعي لانتظار ردّ الشبكة قبل التهيئة.
        syncState(activity, info)
    }

    /** يُعيد فتح النموذج بطلب المستخدم من شاشة الخصوصية. */
    fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                AdLog.consent("تعذّر عرض خيارات الخصوصية: ${formError.message}")
            }
            val info = UserMessagingPlatform.getConsentInformation(activity)
            syncState(activity, info)
            onDone(formError?.message)
        }
    }

    private fun syncState(activity: Activity, info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds()) MobileAdsInitializer.start(activity)
    }
}
