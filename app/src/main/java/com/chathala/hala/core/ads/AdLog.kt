package com.chathala.hala.core.ads

import android.util.Log
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo

/**
 * تسجيل نتائج طلبات AdMob — كانت الإخفاقات صامتة تماماً، فيستحيل معرفة سبب
 * غياب الإعلانات في نسخة الإنتاج (لا امتلاء؟ معرّف خاطئ؟ لا شبكة؟).
 *
 * أكواد الخطأ الشائعة:
 *  0 = خطأ داخلي، 1 = طلب غير صالح (معرّف/تهيئة خاطئة)،
 *  2 = خطأ شبكة، 3 = لا يوجد إعلان متاح (no fill — الأكثر شيوعاً لتطبيق جديد).
 */
internal object AdLog {

    const val TAG = "HalaAds"

    fun failure(placement: String, error: LoadAdError) {
        Log.w(
            TAG,
            "$placement فشل التحميل: code=${error.code} domain=${error.domain} " +
                "msg=${error.message} response=${error.responseInfo?.mediationAdapterClassName}"
        )
    }

    /** أثر دورة حياة خانة البانر — للإجابة عن «هل ضاع إعلان محمّل أم لم يصل أصلاً؟». */
    fun slot(message: String) {
        Log.i(TAG, "خانة: $message")
    }

    fun loaded(placement: String, response: ResponseInfo? = null) {
        Log.i(TAG, "$placement تم تحميل الإعلان network=${response?.mediationAdapterClassName}")
        if (com.chathala.hala.BuildConfig.DEBUG) response?.adapterResponses?.forEach {
            Log.d(TAG, "adapter=${it.adapterClassName} latency=${it.latencyMillis} error=${it.adError?.code}")
        }
    }

    fun consent(message: String) {
        Log.i(TAG, "الموافقة: $message")
    }
}
