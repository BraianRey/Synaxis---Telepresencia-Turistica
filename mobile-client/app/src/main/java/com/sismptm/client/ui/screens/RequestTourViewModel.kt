package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.CreateServiceRequest
import com.sismptm.client.data.remote.RetrofitClient
import com.sismptm.client.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Maps city names to their backend area IDs. */
val CITY_AREA_MAP: Map<String, Long> = mapOf(
    "Popayán"  to 1L,
    "Cali"     to 2L,
    "Medellín" to 3L,
    "Bogotá"   to 4L
)

/** Returns the area ID for the given city name (case-insensitive, accent-insensitive). */
fun cityNameToAreaId(cityName: String): Long? {
    val normalized = cityName.trim().lowercase()
    return CITY_AREA_MAP.entries.firstOrNull {
        it.key.lowercase() == normalized ||
        it.key.lowercase().contains(normalized) ||
        normalized.contains(it.key.lowercase())
    }?.value
}

class RequestTourViewModel : ViewModel() {

    sealed interface RequestUiState {
        object Idle : RequestUiState
        object Loading : RequestUiState
        object Success : RequestUiState
        data class Error(val message: String) : RequestUiState
    }

    private val _uiState = MutableStateFlow<RequestUiState>(RequestUiState.Idle)
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    fun requestTour(
        areaId: Long,
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
                    clientId = clientId,
                    areaId = areaId,
                    startLocationDescription = locationDescription?.ifBlank { null },
                    agreedHours = agreedHours,
                    hourlyRate = hourlyRate
                )
                val response = RetrofitClient.apiService.createService(request)
                if (response.isSuccessful) {
                    _uiState.value = RequestUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    _uiState.value = RequestUiState.Error(
                        when (response.code()) {
                            403 -> "Forbidden (403). Token is valid, but backend denied CLIENT role access."
                            401 -> "Unauthorized (401). Please log in again."
                            else -> "Error ${response.code()}: $errorBody"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RequestUiState.Error(
                    e.localizedMessage ?: "Connection error"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = RequestUiState.Idle
    }
}
