package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.RegisterClientRequest
import com.sismptm.client.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel for the client registration screen.
 * Holds UI state and calls the backend endpoint.
 */
class RegisterViewModel : ViewModel() {

    sealed interface RegisterUiState {
        object Idle : RegisterUiState
        object Loading : RegisterUiState
        object Success : RegisterUiState
        data class Error(val message: String) : RegisterUiState
    }

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(
        name: String,
        email: String,
        password: String,
        termsAccepted: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val language = if (Locale.getDefault().language == "es") "es" else "en"
                val request = RegisterClientRequest(
                    email = email.trim(),
                    password = password,
                    name = name.trim(),
                    termsAccepted = termsAccepted,
                    language = language
                )
                val response = RetrofitClient.apiService.registerClient(request)
                if (response.isSuccessful) {
                    _uiState.value = RegisterUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = RegisterUiState.Error(
                        parseErrorMessage(response.code(), errorBody)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(
                    parseErrorMessage(e)
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }

    private fun parseErrorMessage(code: Int, body: String?): String = when (code) {
        409  -> "Email already registered."
        400  -> "Invalid data. Check all fields."
        else -> "Server error ($code). Please try again."
    }

    private fun parseErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("failed to connect") == true -> {
                "No se pudo conectar. Backend debe estar en http://10.0.2.2:8080"
            }
            exception.message?.contains("timeout") == true -> {
                "Tiempo de conexión agotado. Verifica tu red."
            }
            exception.message?.contains("Connection refused") == true -> {
                "Backend rechazó la conexión. ¿Está corriendo?"
            }
            else -> {
                "Error: ${exception.localizedMessage ?: "Error desconocido"}"
            }
        }
    }
}


