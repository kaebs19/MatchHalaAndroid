package com.chathala.hala.feature.premium.data

import com.squareup.moshi.JsonClass

/**
 * خطط الاشتراك المميّز. معرّفات المنتجات (productId) يجب أن تطابق حرفياً
 * ما يُنشأ في Google Play Console → Subscriptions.
 */
enum class PremiumPlan(
    /** معرّف المنتج في Google Play Console. */
    val productId: String,
    /** القيمة المرسلة للخادم (متوافقة مع premiumPlan في قاعدة البيانات). */
    val serverPlan: String,
    /** اسم معروض للمستخدم. */
    val titleAr: String
) {
    WEEKLY("premium_weekly", "weekly", "أسبوعي"),
    MONTHLY("premium_monthly", "monthly", "شهري"),
    QUARTERLY("premium_quarterly", "quarterly", "ربع سنوي");

    companion object {
        val allProductIds: List<String> = entries.map { it.productId }
        fun fromProductId(id: String?): PremiumPlan? = entries.firstOrNull { it.productId == id }
    }
}

/** طلب التحقق من شراء Google Play وتفعيل الاشتراك على الخادم. */
@JsonClass(generateAdapter = true)
data class GoogleVerifyRequest(
    val purchaseToken: String,
    val productId: String,
    val plan: String,
    val packageName: String
)

/** ردّ التحقق. */
@JsonClass(generateAdapter = true)
data class SubscriptionVerifyResponse(
    val success: Boolean,
    val message: String? = null,
    val data: SubscriptionData? = null
)

@JsonClass(generateAdapter = true)
data class SubscriptionData(
    val isPremium: Boolean = false,
    val plan: String? = null,
    val expiresAt: String? = null
)

/** ردّ حالة الاشتراك الحالية. */
@JsonClass(generateAdapter = true)
data class SubscriptionStatusResponse(
    val success: Boolean,
    val data: SubscriptionData? = null
)
