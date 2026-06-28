package com.chathala.hala.feature.legal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.legal.data.LegalDocType
import com.chathala.hala.feature.legal.data.LegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LegalUiState(
    val loading: Boolean = false,
    val content: String = "",
    val error: String? = null
)

class LegalViewModel(private val repo: LegalRepository) : ViewModel() {

    private val _state = MutableStateFlow(LegalUiState())
    val state: StateFlow<LegalUiState> = _state.asStateFlow()

    fun load(type: LegalDocType) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = when (type) {
                LegalDocType.PRIVACY -> repo.fetchPrivacyPolicy()
                LegalDocType.TERMS -> repo.fetchTerms()
            }
            when (result) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, content = result.data)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HalaApp
                return LegalViewModel(app.legalRepository) as T
            }
        }
    }
}
