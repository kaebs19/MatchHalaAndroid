package com.chathala.hala.feature.settings.ui.account

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.settings.data.AccountStandingData
import com.chathala.hala.feature.settings.data.EscalationStep
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StandingUiState(
    val loading: Boolean = true,
    val data: AccountStandingData? = null,
    val error: String? = null
)

class AccountStandingViewModel(private val tokenStorage: TokenStorage) : ViewModel() {
    private val _state = MutableStateFlow(StandingUiState())
    val state: StateFlow<StandingUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val token = tokenStorage.token.first() ?: throw Exception("no token")
                val resp = ApiClient.service.getAccountStanding("Bearer $token")
                _state.update { it.copy(loading = false, data = resp.data) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                AccountStandingViewModel(app.tokenStorage)
            }
        }
    }
}

private val Orange = Color(0xFFFF8C00)
private val Green = Color(0xFF43A047)
private val Red = Color(0xFFE53935)
private val Amber = Color(0xFFFFB300)

@Composable
fun AccountStandingScreen(
    onBack: () -> Unit,
    onOpenViolations: () -> Unit = {},
    vm: AccountStandingViewModel = viewModel(factory = AccountStandingViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()

    SettingsScaffold(title = "حالة حسابي", onBack = onBack, scrollable = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.data == null -> Text(
                    text = state.error ?: "تعذّر تحميل البيانات",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> StandingContent(state.data!!, onOpenViolations)
            }
        }
    }
}

@Composable
private fun StandingContent(d: AccountStandingData, onOpenViolations: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StandingBadge(d.standing, d.restriction?.hoursLeft ?: 0)
        ViolationsProgress(d.violations, d.softThreshold, onOpenViolations)
        EscalationLadder(d.escalation?.ladder ?: emptyList(), d.escalation?.nextStep ?: 1, d.lockCount)
        d.decay?.daysUntilLockReset?.let { days ->
            if (d.lockCount > 0) RecoveryCard(days)
        }
    }
}

@Composable
private fun StandingBadge(standing: String, hoursLeft: Int) {
    val (emoji, label, color, desc) = when (standing) {
        "good" -> Quad("🟢", "حسابك سليم", Green, "لا توجد قيود على حسابك. واصل الالتزام بسياسة المنصة.")
        "warning" -> Quad("🟡", "تحذير", Amber, "اقتربت من حدّ التقييد. أي مخالفة جديدة قد تقيّد مراسلتك.")
        "restricted" -> Quad("🟠", "مراسلتك مقيّدة", Orange,
            if (hoursLeft > 0) "يتبقّى $hoursLeft ساعة على رفع التقييد." else "حسابك مقيّد مؤقتاً.")
        "suspended" -> Quad("🔴", "حسابك معلّق", Red, "تم تعليق حسابك مؤقتاً بسبب تكرار المخالفات.")
        else -> Quad("⚪", "غير معروف", Color.Gray, "")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
        Spacer(Modifier.size(4.dp))
        Text(
            desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ViolationsProgress(violations: Int, threshold: Int, onOpenViolations: () -> Unit) {
    val pct = if (threshold > 0) (violations.toFloat() / threshold).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(targetValue = pct, label = "violations")
    val barColor = when {
        pct >= 1f -> Red
        pct >= 0.6f -> Orange
        else -> Green
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("المخالفات في الدورة الحالية", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            Text("$violations / $threshold", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = barColor)
        }
        Spacer(Modifier.size(8.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.18f)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (violations >= threshold) "بلغت الحدّ — أي مخالفة تطبّق التصعيد التالي."
            else "تبقّى ${threshold - violations} قبل التقييد.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(10.dp))
        Text(
            "عرض سجل المخالفات ›",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenViolations)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EscalationLadder(ladder: List<EscalationStep>, nextStep: Int, lockCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("سُلّم التصعيد", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.size(4.dp))
        Text(
            "كل تقييد يرفع المدّة. التزامك يحمي حسابك من الحظر الدائم.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(12.dp))
        ladder.forEach { step ->
            val isPast = step.step <= lockCount        // اجتزته بالفعل
            val isNext = step.step == nextStep          // التالي عليك
            val color = when {
                isNext -> Orange
                isPast -> Red
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(color.copy(alpha = if (isNext || isPast) 0.18f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isPast) "✓" else step.step.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                if (isNext) {
                    Text(
                        "التالي",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Orange,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Orange.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecoveryCard(days: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Green.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🌱", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("الاسترداد", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Green)
            Text(
                text = if (days <= 0) "سيُعاد تعيين عدّاد التقييدات قريباً."
                else "سيُعاد تعيين عدّاد التقييدات خلال $days يوم من الالتزام.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Quad(val a: String, val b: String, val c: Color, val d: String)
