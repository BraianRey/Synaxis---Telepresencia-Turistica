package com.sismptm.partner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.data.remote.RetrofitClient
import com.sismptm.partner.data.remote.ServiceResponse
import com.sismptm.partner.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    sealed interface RequestsUiState {
        object Idle : RequestsUiState
        object Loading : RequestsUiState
        data class Success(val requests: List<ServiceResponse>) : RequestsUiState
        data class Error(val message: String) : RequestsUiState
    }

    private val _requestsState = MutableStateFlow<RequestsUiState>(RequestsUiState.Idle)
    val requestsState: StateFlow<RequestsUiState> = _requestsState.asStateFlow()

    private val _acceptedTour = MutableStateFlow<ServiceResponse?>(null)
    val acceptedTour: StateFlow<ServiceResponse?> = _acceptedTour.asStateFlow()

    /** Load available (unassigned) service requests for the partner's area. */
    fun loadAvailableRequests() {
        val areaId = SessionManager.areaId
        if (areaId == 0L) {
            _requestsState.value = RequestsUiState.Idle
            return
        }
        viewModelScope.launch {
            _requestsState.value = RequestsUiState.Loading
            try {
                val response = RetrofitClient.apiService.getServicesAvailableByAreaId(areaId)
                if (response.isSuccessful) {
                    _requestsState.value = RequestsUiState.Success(response.body() ?: emptyList())
                } else {
                    _requestsState.value = RequestsUiState.Error("Error ${response.code()}")
                }
            } catch (e: Exception) {
                _requestsState.value = RequestsUiState.Error(
                    e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Local accept flow (no backend endpoint yet):
     * - removes accepted request from inbox
     * - exposes accepted request details to show confirmation dialog
     */
    fun acceptTour(service: ServiceResponse) {
        val current = _requestsState.value
        if (current is RequestsUiState.Success) {
            _requestsState.value = current.copy(
                requests = current.requests.filterNot { it.serviceId == service.serviceId }
            )
        }
        _acceptedTour.value = service
    }

    fun clearAcceptedTour() {
        _acceptedTour.value = null
    }
}
