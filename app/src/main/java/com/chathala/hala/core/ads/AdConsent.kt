package com.chathala.hala.core.ads

import android.app.Activity
import com.chathala.hala.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * موافقة الخصوصية (UMP / نموذج موافقة Google المعتمد).
 *
 * بدون CMP معتمد، لا يُقدّم خادم الإعلانات إعلاناً لمستخدمي المنطقة الاقتصادية
 * الأوروبية وبريطانيا إطلاقاً (يردّ **No fill**)، ويفقد الطلب إشارات التخصيص في
 * بقية المناطق فتنخفض نسبة التعبئة. النموذج يُنشأ ويُدار من AdMob →
 * Privacy & messaging، وهذا الكود يعرضه عند اللزوم فقط.
 *
 * يُستدعى مرة واحدة من [com.chathala.hala.MainActivity]. آمن للتكرار.
 */
object AdConsent {

    @Volatile
    private var requested = false

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
                }
            },
            { requestError ->
                AdLog.consent("فشل تحديث حالة الموافقة: ${requestError.message}")
            }
        )
    }
}
