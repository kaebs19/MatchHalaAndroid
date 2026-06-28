package com.chathala.hala.feature.settings.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.chathala.hala.feature.settings.data.AppealMessage
import com.chathala.hala.feature.settings.data.AppealReplyRequest
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestDetailUiState(
    val loading: Boolean = true,
    val appeal: AppealItem? = null,
    val sending: Boolean = false,
    val error: String? = null
)

class RequestDetailViewModel(
    private val appealId: String,
    private val tokenStorage: TokenStorage,
    private val socket: com.chathala.hala.feature.chats.socket.HalaSocket
) : ViewModel() {
    private val _state = MutableStateFlow(RequestDetailUiState())
    val state: StateFlow<RequestDetailUiState> = _state.asStateFlow()

    init {
        load()
        observeAdminReplies()
    }

    /** يستمع لردود المشرف اللحظية (appeal-message) ويحدّث المحادثة فوراً. */
    private fun observeAdminReplies() {
        viewModelScope.launch {
            socket.incoming.collect { ev ->
                if (ev is com.chathala.hala.feature.chats.socket.SocketEvent.AppealReply) {
                    val evtAppealId = ev.json.optString("appealId")
                    if (evtAppealId == appealId) {
                        // أعِد التحميل لجلب الرسالة الجديدة وتصفير العداد
                        load()
                    }
                }
            }
        }
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val token = tokenStorage.token.first() ?: throw Exception("no token")
                val resp = ApiClient.service.getAppealDetail("Bearer $token", appealId)
                _state.update { it.copy(loading = false, appeal = resp.data) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun reply(content: String) {
        val text = content.trim()
        if (text.isBlank() || _state.value.sending) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            try {
                val token = tokenStorage.token.first() ?: throw Exception("no token")
                val resp = ApiClient.service.replyToAppeal("Bearer $token", appealId, AppealReplyRequest(text))
                _state.update { it.copy(sending = false, appeal = resp.data ?: it.appeal) }
            } catch (e: Exception) {
                _state.update { it.copy(sending = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(appealId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                RequestDetailViewModel(appealId, app.tokenStorage, app.socket)
            }
        }
    }
}

@Composable
fun RequestDetailScreen(
    appealId: String,
    onBack: () -> Unit,
    vm: RequestDetailViewModel = viewModel(factory = RequestDetailViewModel.factory(appealId))
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    SettingsScaffold(title = "تفاصيل الطلب", onBack = onBack, scrollable = false) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.appeal == null -> Text(
                        state.error ?: "تعذّر تحميل الطلب",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> {
                        val a = state.appeal!!
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item { StatusHeader(a) }
                            items(a.messages) { m -> MessageBubble(m) }
                        }
                    }
                }
            }

            // شريط الرد — يُخفى إذا أُغلق الطلب
            val closed = state.appeal?.status == "approved" || state.appeal?.status == "rejected"
            if (state.appeal != null && !closed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("اكتب ردك للمشرف…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    Spacer(Modifier.size(8.dp))
                    IconButton(
                        onClick = { vm.reply(input); input = "" },
                        enabled = input.isNotBlank() && !state.sending
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else if (closed) {
                Text(
                    text = "تم إغلاق هذا الطلب",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatusHeader(a: AppealItem) {
    val (label, color) = appealStatusLabel(a.status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                appealTypeLabel(a.actionType),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        if (!a.adminNote.isNullOrBlank()) {
            Spacer(Modifier.size(6.dp))
            Text(
                "ملاحظة المشرف: ${a.adminNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            "💬 محادثة مع الإدارة — تواصل مع المشرف لمراجعة الإجراء",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(m: AppealMessage) {
    val isUser = m.sender == "user"
    val bg = if (isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .padding(12.dp)
        ) {
            if (!isUser) {
                Text(
                    "المشرف",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(2.dp))
            }
            Text(m.content ?: "", style = MaterialTheme.typography.bodyMedium, color = fg)
        }
    }
}
