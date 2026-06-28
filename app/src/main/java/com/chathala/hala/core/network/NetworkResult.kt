package com.chathala.hala.core.network

import retrofit2.HttpException
import java.io.IOException

/**
 * Result غلاف عام لاستدعاءات الشبكة — يستخدمه جميع repositories.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * @param payload جسم الخطأ المُحلَّل (ErrorBody) — يحمل تفاصيل إضافية مثل بيانات الإيقاف
     *                عند code = ACCOUNT_SUSPENDED. يبقى null لأخطاء الشبكة/التحليل.
     */
    data class Error(
        val message: String,
        val code: String? = null,
        val payload: Any? = null
    ) : NetworkResult<Nothing>()
}

/**
 * يلتقط أخطاء HTTP/IO بشكل موحّد ويحوّلها إلى NetworkResult.Error برسائل عربية.
 * استخدمه من أي repository بدل تكرار try/catch.
 */
internal inline fun <T> safeApiCall(block: () -> T): NetworkResult<T> = try {
    NetworkResult.Success(block())
} catch (e: HttpException) {
    val raw = e.response()?.errorBody()?.string()
    val parsed = raw?.let {
        try { ApiClient.errorParser.fromJson(it) } catch (_: Exception) { null }
    }
    NetworkResult.Error(
        message = parsed?.message ?: "حدث خطأ (${e.code()})",
        code = parsed?.code,
        payload = parsed
    )
} catch (e: IOException) {
    NetworkResult.Error("تحقق من اتصالك بالإنترنت")
} catch (e: Exception) {
    NetworkResult.Error(e.message ?: "خطأ غير متوقع")
}
