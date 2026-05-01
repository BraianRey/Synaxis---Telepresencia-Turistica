package com.sismptm.partner.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.data.remote.api.dto.RegisterPartnerRequest
import com.sismptm.partner.data.repository.PartnerRepositoryImpl
import com.sismptm.partner.domain.usecase.auth.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

/**
 * ViewModel for partner registration flow, including comprehensive error handling.
 */
class RegisterViewModel(
    private val registerUseCase: RegisterUseCase = RegisterUseCase(PartnerRepositoryImpl())
) : ViewModel() {

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
        longitude: Double,
        latitude: Double,
        termsAccepted: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val language = if (Locale.getDefault().language == "es") "es" else "en"
                val request = RegisterPartnerRequest(
                    email = email.trim(),
                    password = password,
                    name = name.trim(),
                    longitude = longitude,
                    latitude = latitude,
                    termsAccepted = termsAccepted,
                    language = language
                )
                val response = registerUseCase(request)
                if (response.isSuccessful) {
                    _uiState.value = RegisterUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = RegisterUiState.Error(parseError(response.code(), errorBody))
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(parseConnectionError(e))
            }
        }
    }

    private fun parseError(code: Int, body: String?): String {
        val backendMessage = runCatching {
            if (body.isNullOrBlank()) "" else JSONObject(body).optString("error", "")
        }.getOrDefault("")
        
        if (backendMessage.isNotBlank()) return backendMessage
        
        return when (code) {
            409 -> "This email is already registered."
            400 -> "Invalid data. Please verify all fields."
            else -> "Server error ($code). Please try again later."
        }
    }

    private fun parseConnectionError(exception: Exception): String = when {
        exception.message?.contains("failed to connect") == true -> "Connection failed. Please check your internet."
        exception.message?.contains("timeout") == true -> "Request timed out. Please try again."
        else -> "Error: ${exception.localizedMessage ?: "Unknown error"}"
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}

