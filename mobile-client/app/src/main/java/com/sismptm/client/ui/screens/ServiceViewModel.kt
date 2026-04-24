package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.ApiService
import com.sismptm.client.data.remote.CreateServiceRequest
import com.sismptm.client.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CreateServiceUiState {
    object Idle : CreateServiceUiState()
    object Loading : CreateServiceUiState()
    data class Success(val message: String) : CreateServiceUiState()
    data class Error(val message: String) : CreateServiceUiState()
}

class ServiceViewModel : ViewModel() {

    private val _createServiceState = MutableStateFlow<CreateServiceUiState>(CreateServiceUiState.Idle)
    val createServiceState: StateFlow<CreateServiceUiState> = _createServiceState

    fun createService(location: MapLocation, description: String) {
        viewModelScope.launch {
            _createServiceState.value = CreateServiceUiState.Loading

            try {
                val request = CreateServiceRequest(
                    1, //------------------------------------------------------------------provicional
                    longitude = location.lon,
                    latitude = location.lat,
                    startLocationDescription = description.takeIf { it.isNotBlank() },
                    agreedHours = 1,      // ← PROVISIONAL: cambiar con input real
                    hourlyRate = 1.0      // ← PROVISIONAL: cambiar con input real
                )

                val response = RetrofitClient.apiService.createService(request)

                if (response.isSuccessful) {
                    _createServiceState.value = CreateServiceUiState.Success("Service created")
                } else {
                    _createServiceState.value = CreateServiceUiState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _createServiceState.value = CreateServiceUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _createServiceState.value = CreateServiceUiState.Idle
    }
}