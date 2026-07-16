package com.chathala.hala.feature.premium.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.flow.first

/**
 * يربط مشتريات Google Play بالخادم:
 *  - التحقق من الشراء وتفعيل الاشتراك
 *  - جلب حالة الاشتراك الحالية
 */
class SubscriptionRepository(
    private val packageName: String,
    private val userRepository: UserRepository,
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    /**
     * يرسل purchaseToken للخادم للتحقق منه عبر Google Play Developer API وتفعيل premium.
     * عند النجاح يُحدّث بيانات المستخدم المحلية. يُعيد true فقط إذا فعّل الخادم الاشتراك.
     */
    suspend fun verifyGooglePurchase(
        purchaseToken: String,
        plan: PremiumPlan
    ): Boolean {
        val result = safeApiCall {
            api.verifyGoogleSubscription(
                bearer(),
                GoogleVerifyRequest(
                    purchaseToken = purchaseToken,
                    productId = plan.productId,
                    plan = plan.serverPlan,
                    packageName = packageName
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val ok = result.data.success && result.data.data?.isPremium == true
                if (ok) runCatching { userRepository.refresh() }
                ok
            }
            is NetworkResult.Error -> false
        }
    }

    /** حالة الاشتراك الحالية من الخادم. */
    suspend fun fetchStatus(): NetworkResult<SubscriptionData> = safeApiCall {
        val resp = api.getSubscriptionStatus(bearer())
        resp.data ?: SubscriptionData()
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
