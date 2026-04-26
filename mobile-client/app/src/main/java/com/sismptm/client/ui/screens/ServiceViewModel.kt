package com.sismptm.client.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.CreateServiceRequest
import com.sismptm.client.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "ServiceViewModel"

sealed class CreateServiceUiState {
    object Idle : CreateServiceUiState()
    object Loading : CreateServiceUiState()
    data class Success(val serviceId: Long) : CreateServiceUiState()
    data class Error(val message: String) : CreateServiceUiState()
}

class ServiceViewModel : ViewModel() {

    private val _createServiceState = MutableStateFlow<CreateServiceUiState>(CreateServiceUiState.Idle)
    val createServiceState: StateFlow<CreateServiceUiState> = _createServiceState

    fun createService(location: MapLocation, description: String) {
        Log.d(TAG, "▶ createService() called | lat=${location.lat} lon=${location.lon} desc='$description'")
        viewModelScope.launch {
            _createServiceState.value = CreateServiceUiState.Loading
            Log.d(TAG, "⏳ State → Loading")

            try {
                val request = CreateServiceRequest(
                    longitude = location.lon,
                    latitude = location.lat,
                    startLocationDescription = description.takeIf { it.isNotBlank() }
                )
                Log.d(TAG, "📤 Sending request: $request")

                val response = RetrofitClient.apiService.createService(request)
                Log.d(TAG, "📥 Response code: ${response.code()} | successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val serviceId = response.body()?.serviceId
                        ?: throw IllegalStateException("Empty response body")
                    Log.d(TAG, "✅ Service created! serviceId=$serviceId")
                    _createServiceState.value = CreateServiceUiState.Success(serviceId)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "(empty)"
                    Log.e(TAG, "❌ Error ${response.code()}: $errorBody")
                    _createServiceState.value = CreateServiceUiState.Error("Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Exception in createService: ${e.javaClass.simpleName} - ${e.message}", e)
                _createServiceState.value = CreateServiceUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        Log.d(TAG, "🔄 State reset to Idle")
        _createServiceState.value = CreateServiceUiState.Idle
    }
}