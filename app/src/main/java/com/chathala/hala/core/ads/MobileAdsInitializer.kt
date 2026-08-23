package com.chathala.hala.core.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * تهيئة SDK الإعلانات — **بعد** الموافقة، وعلى خيط خلفي.
 *
 * لماذا ليست في `HalaApp.onCreate` كما كانت؟ سببان:
 *
 * 1. **الترتيب**: توثيق UMP صريح — تُجمع الموافقة أوّلاً، ولا يُهيَّأ SDK الإعلانات
 *    إلا بعد أن يصير [com.google.android.ump.ConsentInformation.canRequestAds] صحيحاً.
 *    التهيئة في `Application.onCreate` تسبق حتماً `MainActivity.onCreate` حيث يُجمع
 *    الرضا، فكانت أوّل طلبات الجلسة تخرج بلا إشارات موافقة: رفض تامّ في أوروبا
 *    وبريطانيا، وضعف تخصيص (وتعبئة) في بقيّة المناطق.
 *
 * 2. **الخيط**: `MobileAds.initialize` تقرأ من القرص وتتّصل بالشبكة، واستدعاؤها على
 *    الخيط الرئيسي مصدر ANR معروف عند الإقلاع.
 *
 * آمنة للاستدعاء المتكرّر: أوّل نداء فقط يُنفّذ.
 */
internal object MobileAdsInitializer {

    private val started = AtomicBoolean(false)

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        Thread({
            runCatching { MobileAds.initialize(appContext) {} }
                .onSuccess { Log.i(AdLog.TAG, "تهيئة SDK الإعلانات تمّت") }
                .onFailure { Log.w(AdLog.TAG, "تعذّرت تهيئة SDK الإعلانات: ${it.message}") }
        }, "mobile-ads-init").start()
    }
}
