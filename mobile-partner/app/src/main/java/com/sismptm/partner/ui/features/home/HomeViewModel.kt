package com.sismptm.partner.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.core.session.SessionManager
import com.sismptm.partner.data.remote.api.dto.ServiceResponse
import com.sismptm.partner.data.repository.PartnerRepositoryImpl
import com.sismptm.partner.domain.usecase.tour.AcceptTourUseCase
import com.sismptm.partner.domain.usecase.tour.GetAvailableRequestsUseCase
import com.sismptm.partner.domain.usecase.tour.GetPartnerServicesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for the home screen, managing available tour requests and partner service history.
 */
class HomeViewModel(
    private val getAvailableRequestsUseCase: GetAvailableRequestsUseCase = GetAvailableRequestsUseCase(PartnerRepositoryImpl()),
    private val acceptTourUseCase: AcceptTourUseCase = AcceptTourUseCase(PartnerRepositoryImpl()),
    private val getPartnerServicesUseCase: GetPartnerServicesUseCase = GetPartnerServicesUseCase(PartnerRepositoryImpl())
) : ViewModel() {

    sealed interface RequestsUiState {
        object Idle : RequestsUiState
        object Loading : RequestsUiState
        data class Success(val requests: List<ServiceResponse>) : RequestsUiState
        data class Error(val message: String) : RequestsUiState
    }

    sealed interface PartnerServicesUiState {
        object Idle : PartnerServicesUiState
        object Loading : PartnerServicesUiState
        data class Success(val services: List<ServiceResponse>) : PartnerServicesUiState
        data class Error(val message: String) : PartnerServicesUiState
    }

    private val _requestsState = MutableStateFlow<RequestsUiState>(RequestsUiState.Idle)
    val requestsState: StateFlow<RequestsUiState> = _requestsState.asStateFlow()

    private val _partnerServicesState = MutableStateFlow<PartnerServicesUiState>(PartnerServicesUiState.Idle)
    val partnerServicesState: StateFlow<PartnerServicesUiState> = _partnerServicesState.asStateFlow()

    private val _acceptedTour = MutableStateFlow<ServiceResponse?>(null)
    val acceptedTour: StateFlow<ServiceResponse?> = _acceptedTour.asStateFlow()

    private val _acceptingServiceId = MutableStateFlow<Long?>(null)
    val acceptingServiceId: StateFlow<Long?> = _acceptingServiceId.asStateFlow()

    private val _acceptErrorMessage = MutableStateFlow<String?>(null)
    val acceptErrorMessage: StateFlow<String?> = _acceptErrorMessage.asStateFlow()

    private val _averageRating = MutableStateFlow<String>("-")
    val averageRating: StateFlow<String> = _averageRating.asStateFlow()

    private val _ratingCount = MutableStateFlow(0)
    val ratingCount: StateFlow<Int> = _ratingCount.asStateFlow()

    private val _isLoadingRatings = MutableStateFlow(false)
    val isLoadingRatings: StateFlow<Boolean> = _isLoadingRatings.asStateFlow()

    fun loadAvailableRequests(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _requestsState.value = RequestsUiState.Loading
            try {
                val response = getAvailableRequestsUseCase()
                if (response.isSuccessful) {
                    _requestsState.value = RequestsUiState.Success(response.body() ?: emptyList())
                } else {
                    _requestsState.value = RequestsUiState.Error(parseError(response.code(), response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _requestsState.value = RequestsUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun acceptTour(service: ServiceResponse) {
        viewModelScope.launch {
            _acceptingServiceId.value = service.serviceId
            _acceptErrorMessage.value = null
            try {
                val response = acceptTourUseCase(service.serviceId)
                if (response.isSuccessful) {
                    val acceptedService = response.body() ?: service.copy(status = "ACCEPTED")
                    val current = _requestsState.value
                    if (current is RequestsUiState.Success) {
                        _requestsState.value = current.copy(
                            requests = current.requests.filterNot { it.serviceId == service.serviceId }
                        )
                    }
                    _acceptedTour.value = acceptedService
                    
                    // Refresh partner services to show the accepted service in "My Services"
                    loadPartnerServices()
                } else {
                    _acceptErrorMessage.value = parseError(response.code(), response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _acceptErrorMessage.value = e.localizedMessage ?: "Connection error"
            } finally {
                _acceptingServiceId.value = null
            }
        }
    }

    fun loadPartnerServices(silent: Boolean = false) {
        val partnerId = SessionManager.partnerId
        if (partnerId == 0L) {
            _partnerServicesState.value = PartnerServicesUiState.Error("Session expired. Please log in again.")
            return
        }

        viewModelScope.launch {
            if (!silent) _partnerServicesState.value = PartnerServicesUiState.Loading
            try {
                val response = getPartnerServicesUseCase(partnerId)
                if (response.isSuccessful) {
                    val services = response.body().orEmpty().sortedByDescending { it.serviceId }
                    _partnerServicesState.value = PartnerServicesUiState.Success(services)
                } else {
                    _partnerServicesState.value = PartnerServicesUiState.Error(parseError(response.code(), response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _partnerServicesState.value = PartnerServicesUiState.Error(e.localizedMessage ?: "Connection error")
            }
        }
    }

    private fun parseError(code: Int, body: String?): String {
        val backendMessage = runCatching {
            if (body.isNullOrBlank()) "" else JSONObject(body).optString("error", "")
        }.getOrDefault("")
        if (backendMessage.isNotBlank()) return backendMessage
        return when (code) {
            401 -> "Unauthorized. Please log in again."
            409 -> "This request is no longer available."
            else -> "Server error ($code). Please try again."
        }
    }

    fun loadPartnerRatings() {
        val partnerId = SessionManager.partnerId
        if (partnerId == 0L) return

        viewModelScope.launch {
            _isLoadingRatings.value = true
            try {
                val response = RetrofitClient.apiService.getRatingsByPartner(partnerId)
                if (response.isSuccessful) {
                    val ratings = response.body().orEmpty()
                    if (ratings.isNotEmpty()) {
                        val avg = ratings.map { it.score }.average()
                        _averageRating.value = String.format("%.1f", avg)
                        _ratingCount.value = ratings.size
                    } else {
                        _averageRating.value = "-"
                        _ratingCount.value = 0
                    }
                }
            } catch (e: Exception) {
                // Silent — do not disrupt the home screen
            } finally {
                _isLoadingRatings.value = false
            }
        }
    }

    fun clearAcceptedTour() { _acceptedTour.value = null }
    fun clearAcceptError() { _acceptErrorMessage.value = null }
}
