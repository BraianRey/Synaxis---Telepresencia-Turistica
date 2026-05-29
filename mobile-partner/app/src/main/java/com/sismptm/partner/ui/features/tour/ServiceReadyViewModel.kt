package com.sismptm.partner.ui.features.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.data.repository.PartnerRepositoryImpl
import com.sismptm.partner.domain.usecase.tour.MarkServiceReadyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for the Service Ready screen, handling the transition from acceptance to streaming.
 */
class ServiceReadyViewModel(
    private val startServiceUseCase: MarkServiceReadyUseCase = MarkServiceReadyUseCase(PartnerRepositoryImpl())
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReadyUiState>(ReadyUiState.Idle)
    val uiState: StateFlow<ReadyUiState> = _uiState

    sealed interface ReadyUiState {
        object Idle : ReadyUiState
        object Loading : ReadyUiState
        object Success : ReadyUiState
        data class Error(val message: String) : ReadyUiState
    }

    private val _serviceState = MutableStateFlow<com.sismptm.partner.data.remote.api.dto.ServiceResponse?>(null)
    val serviceState: StateFlow<com.sismptm.partner.data.remote.api.dto.ServiceResponse?> = _serviceState

    // Separate state for the cancel flow so it doesn't interfere with the ready flow
    sealed interface CancelUiState {
        object Idle : CancelUiState
        object Loading : CancelUiState
        object Success : CancelUiState
        data class Error(val message: String) : CancelUiState
    }

    private val _cancelUiState = MutableStateFlow<CancelUiState>(CancelUiState.Idle)
    val cancelUiState: StateFlow<CancelUiState> = _cancelUiState

    fun fetchService(serviceId: Long) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getServiceById(serviceId)
                if (response.isSuccessful) {
                    _serviceState.value = response.body()
                }
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }

    fun markAsReady(serviceId: Long) {
        viewModelScope.launch {
            _uiState.value = ReadyUiState.Loading
            try {
                val response = startServiceUseCase(serviceId)
                if (response.isSuccessful) {
                    _uiState.value = ReadyUiState.Success
                } else {
                    val errorMsg = parseError(response.code(), response.errorBody()?.string())
                    _uiState.value = ReadyUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = ReadyUiState.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun cancelService(serviceId: Long) {
        viewModelScope.launch {
            _cancelUiState.value = CancelUiState.Loading
            try {
                val response = RetrofitClient.apiService.cancelServiceByPartner(serviceId)
                if (response.isSuccessful) {
                    _cancelUiState.value = CancelUiState.Success
                } else {
                    val errorMsg = parseError(response.code(), response.errorBody()?.string())
                    _cancelUiState.value = CancelUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _cancelUiState.value = CancelUiState.Error(e.localizedMessage ?: "Error de conexión al cancelar")
            }
        }
    }

    private fun parseError(code: Int, body: String?): String {
        return try {
            if (body.isNullOrBlank()) "Error $code"
            else JSONObject(body).optString("error", "Server error ($code)")
        } catch (e: Exception) {
            "Server error ($code)"
        }
    }

    fun clearCancelError() {
        _cancelUiState.value = CancelUiState.Idle
    }
}
