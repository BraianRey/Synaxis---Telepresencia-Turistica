package com.sismptm.partner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.data.remote.RegisterPartnerRequest
import com.sismptm.partner.data.remote.RetrofitClient
import com.sismptm.partner.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel for the partner registration screen.
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
        areaId: Int,
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
                    areaId = areaId,
                    termsAccepted = termsAccepted,
                    language = language
                )
                val response = RetrofitClient.apiService.registerPartner(request)
                if (response.isSuccessful) {
                    // Save areaId so HomeScreen can load requests immediately after register
                    SessionManager.areaId = areaId.toLong()
                    _uiState.value = RegisterUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = RegisterUiState.Error(
                        parseErrorMessage(response.code(), errorBody)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(parseErrorMessage(e))
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }

    private fun parseErrorMessage(code: Int, body: String?): String = when (code) {
        409 -> "Email already registered."
        400 -> "Invalid data. Check all fields."
        else -> "Server error ($code). Please try again."
    }

    private fun parseErrorMessage(exception: Exception): String = when {
        exception.message?.contains("failed to connect") == true ->
            "Connection failed. Is the backend running?"
        exception.message?.contains("timeout") == true ->
            "Connection timeout. Check your network."
        else -> "Error: ${exception.localizedMessage ?: "Unknown error"}"
    }
}
