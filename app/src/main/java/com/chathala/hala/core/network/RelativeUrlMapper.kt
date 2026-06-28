package com.chathala.hala.core.network

import coil.map.Mapper
import coil.request.Options

/**
 * يحوّل المسارات النسبية للصور (مثل "/uploads/avatar.jpg") إلى روابط كاملة
 * عبر دمجها مع [ApiClient.BASE_URL]. لا يلمس الروابط التي تبدأ بـ http(s)
 * ولا الـ data URIs ولا الموارد المحلية.
 */
class RelativeUrlMapper : Mapper<String, String> {
    override fun map(data: String, options: Options): String {
        val s = data.trim()
        if (s.isBlank()) return s
        val lower = s.lowercase()
        if (lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("file://") ||
            lower.startsWith("content://") ||
            lower.startsWith("data:") ||
            lower.startsWith("android.resource://")
        ) return s
        val base = ApiClient.BASE_URL.trimEnd('/')
        val path = if (s.startsWith("/")) s else "/$s"
        return "$base$path"
    }
}
