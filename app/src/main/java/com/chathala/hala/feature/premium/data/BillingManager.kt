package com.chathala.hala.feature.premium.data

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * غلاف حول Google Play Billing لإدارة الاشتراكات المميّزة:
 *  - الاتصال بالخدمة (مع إعادة محاولة)
 *  - جلب تفاصيل المنتجات (الأسعار من المتجر)
 *  - إطلاق تدفّق الشراء
 *  - استلام الشراء → التحقق عبر الخادم → الإقرار (acknowledge)
 *  - استرجاع المشتريات (restore) عند فتح التطبيق
 *
 * التحقق الفعلي من الشراء يتم في الخادم عبر [purchaseVerifier] — لا نثق بالعميل.
 * الإقرار (acknowledge) يتم فقط بعد نجاح تحقّق الخادم، وإلا يُلغي Google الشراء تلقائياً خلال 3 أيام.
 */
class BillingManager(private val appContext: Context) {

    private val connecting = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    /** productId → ProductDetails (يحوي الأسعار من المتجر). */
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    /**
     * دالة تحقّق الخادم — تُضبط من HalaApp بعد إنشاء SubscriptionRepository.
     * تُعيد true إذا فعّل الخادم الاشتراك بنجاح → عندها نُقِرّ الشراء.
     */
    var purchaseVerifier: (suspend (Purchase) -> Boolean)? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _events.tryEmit(BillingEvent.Cancelled)
            }
            else -> {
                _events.tryEmit(BillingEvent.Error(result.debugMessage.ifBlank { S.get(R.string.billing_purchase_failed) }))
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /** الاتصال بخدمة الفوترة ثم جلب المنتجات واسترجاع المشتريات. */
    fun connect() {
        if (billingClient.isReady) {
            _connected.value = true
            return
        }
        if (!connecting.compareAndSet(false, true)) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting.set(false)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connected.value = true
                    scope.launch {
                        queryProducts()
                        restorePurchases()
                    }
                } else {
                    _connected.value = false
                    _events.tryEmit(
                        BillingEvent.Error(result.debugMessage.ifBlank { S.get(R.string.billing_play_connect_failed) })
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting.set(false)
                _connected.value = false
                _events.tryEmit(BillingEvent.Error(S.get(R.string.billing_play_connect_failed)))
            }
        })
    }

    /** جلب تفاصيل الاشتراكات الثلاثة من المتجر (الأسعار المحلية). */
    suspend fun queryProducts() {
        if (!billingClient.isReady) return
        val products = PremiumPlan.allProductIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val map = result.productDetailsList
                ?.associateBy { it.productId }
                ?: emptyMap()
            _productDetails.value = map
            if (map.isEmpty()) {
                Log.w(TAG, S.get(R.string.billing_no_products))
            }
        } else {
            _events.tryEmit(
                BillingEvent.Error(result.billingResult.debugMessage.ifBlank { S.get(R.string.billing_fetch_plans_failed) })
            )
        }
    }

    /** إطلاق تدفّق الشراء لخطة محددة. يجب استدعاؤها من Activity. */
    fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        val details = _productDetails.value[plan.productId] ?: run {
            _events.tryEmit(BillingEvent.Error(S.get(R.string.billing_plan_unavailable)))
            return
        }
        // أول عرض متاح للاشتراك (base plan) — نأخذ آخر offerToken (عادةً الأنسب)
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            _events.tryEmit(BillingEvent.Error(S.get(R.string.billing_no_offer)))
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                _events.tryEmit(BillingEvent.Cancelled)
            } else {
                _events.tryEmit(BillingEvent.Error(result.debugMessage.ifBlank { S.get(R.string.billing_purchase_failed) }))
            }
        }
    }

    /**
     * استرجاع المشتريات القائمة (لإعادة تفعيل premium على جهاز جديد أو بعد إعادة تثبيت).
     * تُستدعى تلقائياً بعد الاتصال، ويدوياً من زر «استعادة المشتريات».
     * عند خلوّ الحساب من مشتريات يُبثّ [BillingEvent.RestoreEmpty] — يصل فقط لمن
     * يستمع وقتها (زر الاستعادة)، أما الاسترجاع الصامت عند الإقلاع فلا مستمع له.
     */
    suspend fun restorePurchases() {
        if (!billingClient.isReady) {
            _events.tryEmit(BillingEvent.Error(S.get(R.string.billing_play_connect_failed)))
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = try {
            billingClient.queryPurchasesAsync(params)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _events.tryEmit(BillingEvent.Error(S.get(R.string.billing_play_connect_failed)))
            return
        }
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // نعدّ فقط ما سيُعالج فعلاً — شراء PENDING مثلاً يتجاهله handlePurchase،
            // فلولا هذا الفلتر لبقي زر الاستعادة يدور بلا حدث يُنهيه.
            val restorable = result.purchasesList
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            if (restorable.isEmpty()) {
                _events.tryEmit(BillingEvent.RestoreEmpty)
            } else {
                restorable.forEach { handlePurchase(it) }
            }
        } else {
            _events.tryEmit(BillingEvent.Error(result.billingResult.debugMessage.ifBlank { S.get(R.string.billing_play_connect_failed) }))
        }
    }

    /** معالجة شراء: تحقّق خادم ثم إقرار. */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        scope.launch {
            val verified = try {
                purchaseVerifier?.invoke(purchase) ?: false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { false }
            if (!verified) {
                _events.tryEmit(BillingEvent.Error(S.get(R.string.billing_verify_failed)))
                return@launch
            }
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                var acknowledged = false
                for (attempt in 0 until 3) {
                    val result = billingClient.acknowledgePurchase(ackParams)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        acknowledged = true
                        break
                    }
                    if (attempt < 2) delay(1_000L * (attempt + 1))
                }
                if (!acknowledged) {
                    _events.tryEmit(BillingEvent.Error(S.get(R.string.billing_ack_failed)))
                    return@launch
                }
            }
            _events.tryEmit(BillingEvent.PurchaseVerified)
        }
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}

/** أحداث الفوترة الموجّهة للواجهة. */
sealed interface BillingEvent {
    data object PurchaseVerified : BillingEvent
    data object Cancelled : BillingEvent
    /** طلب استعادة ولم يوجد أي شراء قائم على حساب Google هذا. */
    data object RestoreEmpty : BillingEvent
    data class Error(val message: String) : BillingEvent
}
