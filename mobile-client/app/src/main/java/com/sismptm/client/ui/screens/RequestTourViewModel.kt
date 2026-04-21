package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.CreateServiceRequest
import com.sismptm.client.data.remote.RetrofitClient
import com.sismptm.client.data.remote.ServiceResponse
import com.sismptm.client.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

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
        agreedHours: Int,
        hourlyRate: Double,
        locationDescription: String?
    ) {
        val clientId = SessionManager.clientId
        if (clientId == 0L) {
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
                    startLocationDescription = locationDescription?.ifBlank { null },
                    agreedHours = agreedHours,
                    hourlyRate = hourlyRate
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
            } catch (e: Exception) {
                _uiState.value = RequestUiState.Error(
                    e.localizedMessage ?: "Connection error"
                )
            }
        }
    }

    fun checkActiveServiceBeforeCreate() {
        val clientId = SessionManager.clientId
        if (clientId == 0L) {
            return
        }

        viewModelScope.launch {
            runCatching {
                RetrofitClient.apiService.getServicesByClient(clientId)
            }.onSuccess { servicesResponse ->
                if (!servicesResponse.isSuccessful) {
                    return@onSuccess
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
        val clientId = SessionManager.clientId

        if (clientId == 0L) {
            _uiState.value = RequestUiState.Error(message)
            return
        }

        runCatching {
            RetrofitClient.apiService.getServicesByClient(clientId)
        }.onSuccess { servicesResponse ->
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
        }.onFailure {
            _uiState.value = RequestUiState.Error(message)
        }
    }

    fun resetState() {
        _uiState.value = RequestUiState.Idle
    }

    private fun parseBackendError(body: String): String = runCatching {
        if (body.isBlank()) "" else JSONObject(body).optString("error", "")
    }.getOrDefault("")
}
