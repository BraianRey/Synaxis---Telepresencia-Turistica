package com.sismptm.partner.ui.features.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.data.repository.PartnerRepositoryImpl
import com.sismptm.partner.domain.usecase.tour.MarkServiceReadyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Service Ready screen, handles the transition from acceptance to streaming.
 */
class ServiceReadyViewModel(
    private val markServiceReadyUseCase: MarkServiceReadyUseCase = MarkServiceReadyUseCase(PartnerRepositoryImpl())
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReadyUiState>(ReadyUiState.Idle)
    val uiState: StateFlow<ReadyUiState> = _uiState

    sealed interface ReadyUiState {
        object Idle : ReadyUiState
        object Loading : ReadyUiState
        object Success : ReadyUiState
        data class Error(val message: String) : ReadyUiState
    }

    fun markAsReady(serviceId: Long) {
        viewModelScope.launch {
            _uiState.value = ReadyUiState.Loading
            try {
                val response = markServiceReadyUseCase(serviceId)
                if (response.isSuccessful) {
                    _uiState.value = ReadyUiState.Success
                } else {
                    _uiState.value = ReadyUiState.Error("Could not notify server. Please verify your connection.")
                }
            } catch (e: Exception) {
                _uiState.value = ReadyUiState.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }
}
