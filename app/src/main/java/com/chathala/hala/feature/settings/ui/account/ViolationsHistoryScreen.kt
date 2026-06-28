package com.chathala.hala.feature.settings.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.chathala.hala.feature.settings.data.ViolationEntry
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViolationsUiState(
    val loading: Boolean = true,
    val violations: List<ViolationEntry> = emptyList(),
    val error: String? = null
)

class ViolationsViewModel(private val tokenStorage: TokenStorage) : ViewModel() {
    private val _state = MutableStateFlow(ViolationsUiState())
    val state: StateFlow<ViolationsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val token = tokenStorage.token.first() ?: throw Exception("no token")
                val resp = ApiClient.service.getViolationsHistory("Bearer $token")
                _state.update {
                    it.copy(loading = false, violations = resp.data?.violations ?: emptyList())
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                ViolationsViewModel(app.tokenStorage)
            }
        }
    }
}

@Composable
fun ViolationsHistoryScreen(
    onBack: () -> Unit,
    vm: ViolationsViewModel = viewModel(factory = ViolationsViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<String?>(null) } // null = الكل

    val filtered = remember(state.violations, filter) {
        if (filter == null) state.violations
        else state.violations.filter { it.type == filter }
    }

    SettingsScaffold(title = "سجل المخالفات", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                state.violations.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "لا توجد مخالفات مسجّلة",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    // ── شريط الفلترة ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip("الكل", filter == null) { filter = null }
                        FilterChip("حسابات خارجية", filter == "external_promo") { filter = "external_promo" }
                        FilterChip("كلمات محظورة", filter == "banned_word") { filter = "banned_word" }
                        FilterChip("أخرى", filter == "other") { filter = "other" }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                    ) {
                        items(filtered) { v -> ViolationRow(v) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

@Composable
private fun ViolationRow(v: ViolationEntry) {
    val (icon, iconColor) = when (v.type) {
        "external_promo" -> Pair("🟠", Color(0xFFFF8C00))
        "banned_word" -> Pair("🔴", Color(0xFFE53935))
        "photo" -> Pair("🖼️", Color(0xFF8E24AA))
        else -> Pair("⚠️", Color(0xFF9E9E9E))
    }
    val typeLabel = when (v.type) {
        "external_promo" -> "نشر حساب خارجي"
        "banned_word" -> "كلمة محظورة"
        "photo" -> "صورة مخالفة"
        "name" -> "اسم مخالف"
        "bio" -> "نبذة مخالفة"
        "spam" -> "إزعاج/سبام"
        "report" -> "بلاغ مقبول"
        else -> "مخالفة"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = iconColor
                )
                if (!v.createdAt.isNullOrBlank()) {
                    Text(
                        text = v.createdAt.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            ActionBadge(v.action)
        }

        // الكلمة المكتشفة (مُقنَّعة) + الإجراء التفصيلي
        if (!v.maskedEvidence.isNullOrBlank()) {
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "الكلمة المكتشفة: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = v.maskedEvidence,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = iconColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionBadge(action: String?) {
    val (label, color) = when (action) {
        "warning" -> Pair("تحذير", Color(0xFFFFB300))
        "restricted" -> Pair("تقييد", Color(0xFFFF8C00))
        "suspended" -> Pair("تعليق", Color(0xFFE53935))
        "banned" -> Pair("حظر", Color(0xFFB71C1C))
        "photo_removed" -> Pair("حذف صورة", Color(0xFF8E24AA))
        else -> return
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
