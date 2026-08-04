package com.chathala.hala.core.network

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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
     * @param httpCode رمز HTTP (404 مثلاً) — يبقى null لأخطاء الشبكة/التحليل.
     */
    data class Error(
        val message: String,
        val code: String? = null,
        val payload: Any? = null,
        val httpCode: Int? = null
    ) : NetworkResult<Nothing>() {
        /** المورد غير موجود — عملياً: حساب محذوف (الخادم يحذف الوثيقة نهائياً). */
        val isNotFound: Boolean get() = httpCode == 404
    }
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
        message = parsed?.message ?: S.get(R.string.err_generic_code, e.code().toString()),
        code = parsed?.code,
        payload = parsed,
        httpCode = e.code()
    )
} catch (e: IOException) {
    NetworkResult.Error(S.get(R.string.err_check_connection))
} catch (e: Exception) {
    NetworkResult.Error(e.message ?: S.get(R.string.err_unexpected))
}
