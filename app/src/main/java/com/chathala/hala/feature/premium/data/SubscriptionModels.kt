package com.chathala.hala.feature.premium.data

import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
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
    /** مورد الاسم المعروض — نخزّن المعرّف لا النص كي لا تتجمّد اللغة عند تحميل الصنف. */
    private val titleRes: Int
) {
    WEEKLY("premium_weekly", "weekly", R.string.plan_weekly),
    MONTHLY("premium_monthly", "monthly", R.string.plan_monthly),
    QUARTERLY("premium_quarterly", "quarterly", R.string.plan_quarterly);

    /** الاسم المعروض باللغة الحالية. */
    val title: String get() = S.get(titleRes)

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
