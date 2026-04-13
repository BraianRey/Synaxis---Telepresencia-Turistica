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
import org.json.JSONObject

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

    private val _acceptingServiceId = MutableStateFlow<Long?>(null)
    val acceptingServiceId: StateFlow<Long?> = _acceptingServiceId.asStateFlow()

    private val _acceptErrorMessage = MutableStateFlow<String?>(null)
    val acceptErrorMessage: StateFlow<String?> = _acceptErrorMessage.asStateFlow()

    /** Load available (unassigned) service requests for the partner's area.
     *  @param silent when true, skip setting Loading state (used for background polling). */
    fun loadAvailableRequests(silent: Boolean = false) {
        val areaId = SessionManager.areaId
        if (areaId == 0L) {
            _requestsState.value = RequestsUiState.Idle
            return
        }
        viewModelScope.launch {
            if (!silent) {
                _requestsState.value = RequestsUiState.Loading
            }
            try {
                val response = RetrofitClient.apiService.getServicesAvailableByAreaId(areaId)
                if (response.isSuccessful) {
                    _requestsState.value = RequestsUiState.Success(response.body() ?: emptyList())
                } else {
                    _requestsState.value = RequestsUiState.Error(
                        parseBackendError(response.code(), response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                _requestsState.value = RequestsUiState.Error(
                    e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    fun acceptTour(service: ServiceResponse) {
        viewModelScope.launch {
            _acceptingServiceId.value = service.serviceId
            _acceptErrorMessage.value = null
            try {
                val response = RetrofitClient.apiService.acceptService(service.serviceId)
                if (response.isSuccessful) {
                    val acceptedService = response.body() ?: service.copy(status = "ACCEPTED")
                    val current = _requestsState.value
                    if (current is RequestsUiState.Success) {
                        _requestsState.value = current.copy(
                            requests = current.requests.filterNot { it.serviceId == service.serviceId }
                        )
                    }
                    _acceptedTour.value = acceptedService
                } else {
                    _acceptErrorMessage.value = parseBackendError(
                        response.code(),
                        response.errorBody()?.string()
                    )
                }
            } catch (e: Exception) {
                _acceptErrorMessage.value = e.localizedMessage ?: "Connection error"
            } finally {
                _acceptingServiceId.value = null
            }
        }
    }

    fun clearAcceptedTour() {
        _acceptedTour.value = null
    }

    fun clearAcceptError() {
        _acceptErrorMessage.value = null
    }

    private fun parseBackendError(code: Int, body: String?): String {
        val backendMessage = runCatching {
            if (body.isNullOrBlank()) "" else JSONObject(body).optString("error", "")
        }.getOrDefault("")

        if (backendMessage.isNotBlank()) return backendMessage

        return when (code) {
            401 -> "Unauthorized. Please log in again."
            403 -> "You do not have permission to perform this action."
            404 -> "Service request not found."
            409 -> "This request is no longer available. Refresh the list and try again."
            else -> "Server error ($code). Please try again."
        }
    }
}