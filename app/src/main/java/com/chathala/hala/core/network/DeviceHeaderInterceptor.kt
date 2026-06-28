package com.chathala.hala.core.network

import com.chathala.hala.core.device.DeviceIdentity
import okhttp3.Interceptor
import okhttp3.Response

/**
 * يُرفِق معرّفات الجهاز كترويسات على كل طلب — يفعّل:
 *  - `middleware/bannedDeviceCheck` (يقرأ x-device-fingerprint / x-device-token / x-vendor-id)
 *  - `middleware/fingerprintUpdater` (يقرأ x-device-fingerprint / x-keychain-token)
 *
 * يقرأ القيم من [DeviceIdentity] لحظياً؛ لو لم تُهيّأ بعد يتخطّى الترويسة بأمان.
 */
class DeviceHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()

        DeviceIdentity.deviceFingerprint.takeIf { it.isNotEmpty() }?.let {
            builder.header("x-device-fingerprint", it)
        }
        DeviceIdentity.deviceToken.takeIf { it.isNotEmpty() }?.let {
            builder.header("x-device-token", it)
            builder.header("x-keychain-token", it)
        }
        DeviceIdentity.vendorId.takeIf { it.isNotEmpty() }?.let {
            builder.header("x-vendor-id", it)
        }
        builder.header("x-app-platform", DeviceIdentity.platform)
        // ملاحظة: لا نرسل x-app-version حالياً. الباك إند (middleware/versionCheck)
        // يقارن x-app-version بحدّ أدنى مضبوط لنسخة iOS (مثل 7.0)، فيرفض نسخة أندرويد
        // الحالية (versionName منخفض) بـ 426 ويحجب التطبيق بالكامل. حظر الجهاز لا يحتاج
        // هذه الترويسة. أعِد تفعيلها بعد ضبط حدّ أدنى خاص بمنصّة أندرويد في إعدادات السيرفر.

        return chain.proceed(builder.build())
    }
}
