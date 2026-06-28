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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.chathala.hala.feature.settings.data.AppealItem
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyRequestsUiState(
    val loading: Boolean = true,
    val appeals: List<AppealItem> = emptyList(),
    val submitting: Boolean = false,
    val reviewable: Boolean = true,
    val nonReviewableNote: String? = null,
    val message: String? = null,
    val error: String? = null
) {
    /** يوجد طلب مفتوح بالفعل → نخفي نموذج الإرسال. */
    val hasOpenRequest: Boolean
        get() = appeals.any { it.status in listOf("pending", "forwarded", "under_review") }
}

class MyRequestsViewModel(private val tokenStorage: TokenStorage) : ViewModel() {
    private val _state = MutableStateFlow(MyRequestsUiState())
    val state: StateFlow<MyRequestsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val token = tokenStorage.token.first() ?: throw Exception("no token")
                val resp = ApiClient.service.getMyAppeals("Bearer $token")
                // قابلية المراجعة من حالة الحساب (بعض المخالفات غير قابلة للمراجعة)
                val standing = runCatching {
                    ApiClient.service.getAccountStanding("Bearer $token").data
                }.getOrNull()
                _state.update {
                    it.copy(
                        loading = false,
                        appeals = resp.data,
                        reviewable = standing?.reviewable ?: true,
                        nonReviewableNote = standing?.nonReviewableNote
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun consumeMessage() { _state.update { it.copy(message = null) } }

    /** تقديم طلب مراجعة جديد — نفس نظام /api/appeals بـ actionType=restriction. */
    fun submit(reason: String) {
        val text = reason.trim()
        if (text.isBlank() || _state.value.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                val token = tokenStorage.token.first() ?: throw Exception("no token")
                val resp = ApiClient.service.submitAppeal(
                    "Bearer $token",
                    com.chathala.hala.feature.suspension.data.AppealRequest(
                        reason = text, actionType = "restriction"
                    )
                )
                _state.update {
                    it.copy(
                        submitting = false,
                        message = resp.message ?: "تم إرسال طلبك، سيراجعه المشرف"
                    )
                }
                load() // أعد تحميل القائمة لإظهار الطلب الجديد
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, message = "تعذّر الإرسال، حاول لاحقاً") }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                MyRequestsViewModel(app.tokenStorage)
            }
        }
    }
}

/** ترجمة الحالة + اللون. */
internal fun appealStatusLabel(status: String?): Pair<String, Color> = when (status) {
    "pending" -> "قيد الانتظار" to Color(0xFFFFB300)
    "forwarded" -> "محوّل" to Color(0xFF42A5F5)
    "under_review" -> "قيد المراجعة" to Color(0xFFFF8C00)
    "approved" -> "مقبول" to Color(0xFF43A047)
    "rejected" -> "مرفوض" to Color(0xFFE53935)
    else -> "—" to Color.Gray
}

internal fun appealTypeLabel(actionType: String?): String = when (actionType) {
    "restriction" -> "تقييد المراسلة"
    "suspension" -> "تعليق الحساب"
    "ban" -> "حظر الحساب"
    "device_ban" -> "حظر الجهاز"
    else -> "مراجعة"
}

@Composable
fun MyRequestsScreen(
    onBack: () -> Unit,
    onOpenRequest: (String) -> Unit,
    onOpenPrivacy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    onOpenViolations: () -> Unit = {},
    vm: MyRequestsViewModel = viewModel(factory = MyRequestsViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHost = com.chathala.hala.ui.components.rememberHalaSnackbarHost()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScaffold(title = "طلباتي", onBack = onBack, scrollable = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // روابط سريعة: الخصوصية / الشروط / اتصل بنا / إنستا
                        item {
                            QuickLinksRow(
                                onOpenPrivacy = onOpenPrivacy,
                                onOpenTerms = onOpenTerms,
                                onOpenContact = onOpenContact,
                                onOpenViolations = onOpenViolations,
                                onOpenInstagram = {
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(
                                                    com.chathala.hala.core.config.OfficialContacts.INSTAGRAM
                                                )
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        // نموذج تقديم طلب جديد:
                        //  - إذا المخالفة غير قابلة للمراجعة → بطاقة توضيحية
                        //  - وإلا (ولا يوجد طلب مفتوح) → نموذج الإرسال
                        if (!state.reviewable) {
                            item { NonReviewableCard(note = state.nonReviewableNote) }
                        } else if (!state.hasOpenRequest) {
                            item { SubmitRequestCard(submitting = state.submitting, onSubmit = vm::submit) }
                        }
                        if (state.appeals.isNotEmpty()) {
                            item {
                                Text(
                                    "الطلبات السابقة",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(state.appeals) { a -> RequestRow(a) { onOpenRequest(a.id) } }
                        } else {
                            item {
                                Text(
                                    "لا توجد طلبات سابقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        com.chathala.hala.ui.components.HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun QuickLinksRow(
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenViolations: () -> Unit,
    onOpenInstagram: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickLinkChip("⚠️ سجل المخالفات", Color(0xFFFF8C00), onOpenViolations)
        QuickLinkChip("📄 الشروط", Color(0xFF42A5F5), onOpenTerms)
        QuickLinkChip("🔒 الخصوصية", Color(0xFF43A047), onOpenPrivacy)
        QuickLinkChip("✉️ اتصل بنا", Color(0xFF7E57C2), onOpenContact)
        QuickLinkChip("📷 إنستا", Color(0xFFE1306C), onOpenInstagram)
    }
}

@Composable
private fun QuickLinkChip(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun NonReviewableCard(note: String?) {
    val red = Color(0xFFE53935)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(red.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🚫", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(10.dp))
            Text(
                "هذه المخالفة غير قابلة للمراجعة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = red
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = note
                ?: "مخالفات الصور الإباحية أو الجنسية والأسماء المخالفة غير قابلة للمراجعة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubmitRequestCard(submitting: Boolean, onSubmit: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            "قدّم طلب مراجعة",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.size(4.dp))
        Text(
            "ابدأ محادثة مع فريق الإدارة لإعادة النظر في الإجراء المتخذ ضد حسابك.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(12.dp))
        androidx.compose.material3.OutlinedTextField(
            value = reason,
            onValueChange = { if (it.length <= 1000) reason = it },
            placeholder = { Text("اكتب أول رسالة للإدارة…") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            supportingText = {
                Text("${reason.length}/1000", style = MaterialTheme.typography.labelSmall)
            }
        )
        Spacer(Modifier.size(12.dp))
        androidx.compose.material3.Button(
            onClick = { onSubmit(reason); reason = "" },
            enabled = reason.isNotBlank() && !submitting,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("إرسال الطلب", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun RequestRow(a: AppealItem, onClick: () -> Unit) {
    val (statusLabel, statusColor) = appealStatusLabel(a.status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appealTypeLabel(a.actionType),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if ((a.unreadForUser ?: 0) > 0) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE53935))
                    )
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        if (!a.reason.isNullOrBlank()) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = a.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        if (!a.createdAt.isNullOrBlank()) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = a.createdAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
