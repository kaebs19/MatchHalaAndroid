package com.chathala.hala.feature.premium.ui

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.feature.premium.data.PremiumPlan
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

private val Gold = Color(0xFFFFC107)
private val GoldDark = Color(0xFFFFA726)

/** مزايا الاشتراك المميّز المعروضة في الشاشة. */
private val premiumBenefits = listOf(
    S.get(R.string.premium_perk_superlikes),
    S.get(R.string.premium_perk_top_placement),
    S.get(R.string.premium_perk_see_likes),
    S.get(R.string.premium_perk_stealth),
    S.get(R.string.premium_perk_gold_badge),
    S.get(R.string.premium_perk_no_ads)
)

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    viewModel: SubscriptionViewModel = viewModel(factory = SubscriptionViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }
    LaunchedEffect(state.success) {
        if (state.success) onBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // شريط علوي
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.get(R.string.action_back))
                }
            }

            // هيدر مميّز
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Gold, GoldDark)))
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = S.get(R.string.premium_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = S.get(R.string.premium_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            // المزايا (مبسّطة وأكثر تراصّاً)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                premiumBenefits.forEach { benefit ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // الباقات
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.plans.isEmpty()) {
                    Text(
                        text = if (state.connected) S.get(R.string.premium_loading_plans) else S.get(R.string.premium_connecting_store),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    state.plans.forEach { option ->
                        PlanCard(
                            title = option.plan.title,
                            price = option.formattedPrice,
                            selected = state.selectedPlan == option.plan,
                            onClick = { viewModel.selectPlan(option.plan) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // زر الاشتراك
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                HalaPrimaryButton(
                    text = S.get(R.string.premium_subscribe_now),
                    onClick = { activity?.let { viewModel.purchase(it) } },
                    loading = state.purchasing,
                    enabled = state.plans.isNotEmpty() && activity != null
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = S.get(R.string.premium_renewal_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(16.dp))
            // روابط قانونية + اتصل بنا
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinkText(S.get(R.string.legal_terms_of_use), onOpenTerms)
                LinkDot()
                LinkText(S.get(R.string.legal_privacy_policy), onOpenPrivacy)
                LinkDot()
                LinkText(S.get(R.string.premium_contact_us), onOpenContact)
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
private fun LinkDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Gold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) Gold.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
            )
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                .background(if (selected) Gold else Color.Transparent)
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.size(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = price,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) GoldDark else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** يستخرج الـ Activity من الـ Context المتشعّب (لإطلاق تدفّق الشراء). */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
