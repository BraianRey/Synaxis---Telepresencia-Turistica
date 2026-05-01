package com.sismptm.client.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.core.network.NetworkConfig
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.data.remote.api.dto.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for the login screen.
 * Updated to use the new core infrastructure.
 */
class LoginViewModel : ViewModel() {

    sealed interface LoginUiState {
        object Idle : LoginUiState
        object Loading : LoginUiState
        object Success : LoginUiState
        data class Error(val message: String) : LoginUiState
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Attempts to log in with the provided email and password.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = RetrofitClient.apiService.loginClient(
                    LoginRequest(email = email.trim(), password = password)
                )
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        // Using the new unified SessionManager
                        SessionManager.saveSession(
                            token = loginResponse.accessToken,
                            id = loginResponse.id,
                            name = loginResponse.name,
                            email = loginResponse.email,
                            role = loginResponse.role,
                            lang = loginResponse.language
                        )
                    }
                    _uiState.value = LoginUiState.Success
                } else {
                    _uiState.value = LoginUiState.Error(
                        parseError(response.code(), response.errorBody()?.string())
                    )
                }
            } catch (ex: Exception) {
                _uiState.value = LoginUiState.Error(parseConnectionError(ex))
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    private fun parseError(code: Int, body: String?): String {
        val backendMessage = runCatching {
            if (body.isNullOrBlank()) "" else JSONObject(body).optString("error", "")
        }.getOrDefault("")
        
        if (backendMessage.isNotBlank()) return backendMessage
        return if (code == 401) "Invalid credentials." else "Server error. Please try again."
    }

    private fun parseConnectionError(exception: Exception): String {
        val baseUrl = NetworkConfig.BASE_URL
        return when {
            exception.message?.contains("failed to connect", ignoreCase = true) == true ->
                "Could not connect to the server at $baseUrl."
            exception.message?.contains("timeout", ignoreCase = true) == true ->
                "Connection timed out. Please check your network."
            exception.message?.contains("refused", ignoreCase = true) == true ->
                "Connection refused. Ensure the backend server is running."
            else -> "Error: ${exception.localizedMessage ?: "Unknown error"}"
        }
    }
}

