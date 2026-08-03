package com.chathala.hala.core.ads

import android.util.Log
import com.google.android.gms.ads.LoadAdError

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

    fun loaded(placement: String) {
        Log.i(TAG, "$placement تم تحميل الإعلان")
    }
}
