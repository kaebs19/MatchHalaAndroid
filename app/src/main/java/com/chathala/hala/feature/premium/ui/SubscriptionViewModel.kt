package com.chathala.hala.feature.premium.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.chathala.hala.HalaApp
import com.chathala.hala.feature.premium.data.BillingEvent
import com.chathala.hala.feature.premium.data.BillingManager
import com.chathala.hala.feature.premium.data.PremiumPlan
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val connected: Boolean = false,
    val plans: List<PlanOption> = emptyList(),
    val selectedPlan: PremiumPlan = PremiumPlan.MONTHLY,
    val purchasing: Boolean = false,
    val success: Boolean = false
)

/** خيار باقة معروض للمستخدم (يحوي السعر من المتجر). */
data class PlanOption(
    val plan: PremiumPlan,
    val formattedPrice: String,
    val details: ProductDetails
)

class SubscriptionViewModel(
    private val billing: BillingManager
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        // تتبّع الاتصال
        viewModelScope.launch {
            billing.connected.collect { c -> _state.update { it.copy(connected = c) } }
        }
        // تتبّع تفاصيل المنتجات (الأسعار)
        viewModelScope.launch {
            billing.productDetails.collect { map ->
                val options = PremiumPlan.entries.mapNotNull { plan ->
                    val d = map[plan.productId] ?: return@mapNotNull null
                    val price = d.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        ?.formattedPrice ?: "—"
                    PlanOption(plan, price, d)
                }
                _state.update { it.copy(plans = options) }
            }
        }
        // أحداث الفوترة
        viewModelScope.launch {
            billing.events.collect { event ->
                when (event) {
                    is BillingEvent.PurchaseVerified -> {
                        _state.update { it.copy(purchasing = false, success = true) }
                        _message.tryEmit("تم تفعيل اشتراكك المميّز 🎉")
                    }
                    is BillingEvent.Cancelled -> {
                        _state.update { it.copy(purchasing = false) }
                    }
                    is BillingEvent.Error -> {
                        _state.update { it.copy(purchasing = false) }
                        _message.tryEmit(event.message)
                    }
                }
            }
        }
        billing.connect()
    }

    fun selectPlan(plan: PremiumPlan) {
        _state.update { it.copy(selectedPlan = plan) }
    }

    fun purchase(activity: Activity) {
        _state.update { it.copy(purchasing = true) }
        billing.launchPurchase(activity, _state.value.selectedPlan)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return SubscriptionViewModel(billing = app.billingManager) as T
            }
        }
    }
}
