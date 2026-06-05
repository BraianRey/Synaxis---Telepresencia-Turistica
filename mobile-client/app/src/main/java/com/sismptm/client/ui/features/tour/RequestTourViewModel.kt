package com.sismptm.client.ui.features.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.data.remote.api.dto.CreateServiceRequest
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Log

class RequestTourViewModel : ViewModel() {

    private val activeStatuses = setOf("REQUESTED", "ACCEPTED", "STARTED")

    sealed interface RequestUiState {
        object Idle : RequestUiState
        object Loading : RequestUiState
        data class Success(val service: ServiceResponse) : RequestUiState
        data class ActiveService(val service: ServiceResponse, val message: String) : RequestUiState
        data class Error(val message: String) : RequestUiState
    }

    private val _uiState = MutableStateFlow<RequestUiState>(RequestUiState.Idle)
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    fun requestTour(
        longitude: Double,
        latitude: Double,
        locationDescription: String?
    ) {
        val clientId = SessionManager.userId
        if (clientId <= 0L) {
            _uiState.value = RequestUiState.Error("Session expired. Please log in again.")
            return
        }

        // Backend endpoint requires hasRole('CLIENT')
        if (!SessionManager.userRole.equals("CLIENT", ignoreCase = true)) {
            _uiState.value = RequestUiState.Error(
                "Your account role is '${SessionManager.userRole}'. Only CLIENT can request tours."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = RequestUiState.Loading
            try {
                val request = CreateServiceRequest(
                    longitude = longitude,
                    latitude = latitude,
                    startLocationDescription = locationDescription?.ifBlank { null }
                )
                val response = RetrofitClient.apiService.createService(request)
                if (response.isSuccessful) {
                    val service = response.body()
                    if (service != null) {
                        _uiState.value = RequestUiState.Success(service)
                    } else {
                        _uiState.value = RequestUiState.Error("Server returned an empty response.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    when (response.code()) {
                        409 -> resolveActiveServiceConflict(errorBody)
                        403 -> _uiState.value = RequestUiState.Error(
                            "Forbidden (403). Token is valid, but backend denied CLIENT role access."
                        )
                        401 -> _uiState.value = RequestUiState.Error("Unauthorized (401). Please log in again.")
                        else -> _uiState.value = RequestUiState.Error(
                            parseBackendError(errorBody).ifBlank {
                                "Error ${response.code()}: $errorBody"
                            }
                        )
                    }
                }
            } catch (e: java.io.IOException) {
                // Generic exception kept: Network error requesting tour
                Log.e("RequestTourViewModel", "Failed to request tour", e)
                _uiState.value = RequestUiState.Error(
                    e.localizedMessage ?: "Connection error"
                )
            }
        }
    }

    fun checkActiveServiceBeforeCreate() {
        val clientId = SessionManager.userId
        if (clientId <= 0L) {
            return
        }

        viewModelScope.launch {
            try {
                val servicesResponse = RetrofitClient.apiService.getServicesByClient(clientId)
                if (!servicesResponse.isSuccessful) {
                    return@launch
                }

                val activeService = servicesResponse.body()
                    ?.filter { it.status.uppercase() in activeStatuses }
                    ?.maxByOrNull { it.serviceId }

                if (activeService != null) {
                    _uiState.value = RequestUiState.ActiveService(
                        activeService,
                        "You already have an active service request."
                    )
                }
            } catch (e: java.io.IOException) {
                // Generic exception kept: Network error checking active services
                Log.e("RequestTourViewModel", "Error checking active services", e)
            }
        }
    }

    private suspend fun resolveActiveServiceConflict(errorBody: String) {
        val conflictMessage = parseBackendError(errorBody).ifBlank {
            "You already have an active service request."
        }
        resolveActiveService(conflictMessage)
    }

    private suspend fun resolveActiveService(message: String) {
        val clientId = SessionManager.userId

        if (clientId <= 0L) {
            _uiState.value = RequestUiState.Error(message)
            return
        }

        try {
            val servicesResponse = RetrofitClient.apiService.getServicesByClient(clientId)
            if (!servicesResponse.isSuccessful) {
                _uiState.value = RequestUiState.Error(message)
                return
            }

            val activeService = servicesResponse.body()
                ?.filter { it.status.uppercase() in activeStatuses }
                ?.maxByOrNull { it.serviceId }

            if (activeService != null) {
                _uiState.value = RequestUiState.ActiveService(activeService, message)
            } else {
                _uiState.value = RequestUiState.Error(message)
            }
        } catch (e: java.io.IOException) {
            // Generic exception kept: Network error resolving active service
            Log.e("RequestTourViewModel", "Failed to resolve active service", e)
            _uiState.value = RequestUiState.Error(message)
        }
    }

    fun resetState() {
        _uiState.value = RequestUiState.Idle
    }

    private fun parseBackendError(body: String): String {
        if (body.isBlank()) return ""
        return try {
            JSONObject(body).optString("error", "")
        } catch (e: org.json.JSONException) {
            Log.e("RequestTourViewModel", "Error parsing JSON", e)
            ""
        }
    }
}

