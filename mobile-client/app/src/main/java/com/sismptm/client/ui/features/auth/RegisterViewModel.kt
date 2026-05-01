package com.sismptm.client.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.core.network.NetworkConfig
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.data.remote.api.dto.RegisterClientRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel for the client registration flow.
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

    /**
     * Registers a new user with the provided information.
     */
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

    /**
     * Resets the UI state to Idle.
     */
    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }

    private fun parseErrorMessage(code: Int, body: String?): String = when (code) {
        409  -> "Email already registered."
        400  -> "Invalid data. Please check all fields."
        else -> "Server error ($code). Please try again."
    }

    private fun parseErrorMessage(exception: Exception): String {
        val baseUrl = NetworkConfig.BASE_URL
        return when {
            exception.message?.contains("failed to connect", ignoreCase = true) == true -> {
                "Could not connect to the server at $baseUrl."
            }
            exception.message?.contains("timeout", ignoreCase = true) == true -> {
                "Connection timed out. Please check your internet."
            }
            exception.message?.contains("refused", ignoreCase = true) == true -> {
                "Connection refused. Ensure the backend server is running."
            }
            else -> {
                "Error: ${exception.localizedMessage ?: "An unexpected error occurred."}"
            }
        }
    }
}
