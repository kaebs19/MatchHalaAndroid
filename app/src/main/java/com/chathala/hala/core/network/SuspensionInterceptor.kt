package com.chathala.hala.core.network

import com.chathala.hala.feature.auth.data.ErrorBody
import com.chathala.hala.feature.suspension.data.AccountSuspensionInfo
import com.chathala.hala.feature.suspension.data.SuspensionGate
import com.chathala.hala.feature.suspension.data.SuspensionMode
import okhttp3.Interceptor
import okhttp3.Response

/**
 * يلتقط تعليق الحساب/حظر الجهاز أثناء الجلسة: أي طلب (غير مسار /api/auth الذي
 * يعالجه AuthViewModel) يرجع 403 بـ code = ACCOUNT_SUSPENDED أو DEVICE_BANNED
 * → يضبط [SuspensionGate] ويُطلق حدثاً يلتقطه NavGraph للتوجيه لشاشة الحظر.
 *
 * يستخدم peekBody حتى لا يستهلك جسم الرد (يبقى متاحاً للطبقات الأعلى).
 */
class SuspensionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 403) return response
        // مسارات المصادقة يعالجها تدفّق تسجيل الدخول مباشرة
        if (request.url.encodedPath.contains("/api/auth/")) return response

        val body = runCatching { response.peekBody(MAX_PEEK).string() }.getOrNull() ?: return response
        val parsed = runCatching { ApiClient.errorParser.fromJson(body) }.getOrNull() ?: return response

        when (parsed.code) {
            "ACCOUNT_SUSPENDED" -> {
                val until = parsed.data?.suspendedUntil
                SuspensionGate.publish(
                    info = AccountSuspensionInfo(
                        name = parsed.user?.name,
                        email = parsed.user?.email,
                        userId = parsed.user?.id,
                        reason = parsed.data?.reason,
                        suspendedUntil = until,
                        level = parsed.data?.level ?: 0,
                        isPermanent = until.isNullOrBlank(),
                        // أثناء الجلسة نستخدم توكن الجلسة المخزّن لتقديم الاستئناف
                        token = null
                    ),
                    mode = SuspensionMode.ACCOUNT
                )
            }
            "DEVICE_BANNED" -> SuspensionGate.publish(null, SuspensionMode.DEVICE)
        }

        return response
    }

    private companion object {
        const val MAX_PEEK = 64L * 1024L
    }
}
