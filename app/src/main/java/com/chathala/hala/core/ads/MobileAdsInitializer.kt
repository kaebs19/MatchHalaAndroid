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
        if (com.chathala.hala.BuildConfig.DEBUG) {
            MobileAds.setRequestConfiguration(MobileAds.getRequestConfiguration().toBuilder()
                .setTestDeviceIds(com.chathala.hala.BuildConfig.AD_TEST_DEVICE_IDS.toList()).build())
        }
        // Keep Activity context for mediated networks that require it during initialization.
        Thread({
            runCatching {
                MobileAds.initialize(context) { status ->
                    AdGate.updateSdkReady(true)
                    if (com.chathala.hala.BuildConfig.DEBUG) {
                        status.adapterStatusMap.forEach { (name, adapter) ->
                            Log.d(AdLog.TAG, "adapter=$name state=${adapter.initializationState} latency=${adapter.latency}")
                        }
                    }
                }
            }.onFailure {
                started.set(false)
                AdGate.updateSdkReady(false)
                Log.w(AdLog.TAG, "تعذّرت تهيئة SDK الإعلانات: ${it.message}")
            }
        }, "mobile-ads-init").start()
    }
}
