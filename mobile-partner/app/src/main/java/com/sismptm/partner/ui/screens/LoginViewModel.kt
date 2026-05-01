package com.sismptm.partner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.core.session.SessionManager
import com.sismptm.partner.data.remote.api.dto.LoginRequest
import com.sismptm.partner.domain.usecase.auth.LoginUseCase
import com.sismptm.partner.domain.usecase.auth.CheckServerStatusUseCase
import com.sismptm.partner.data.repository.PartnerRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for the login screen, managing authentication and server availability checks.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase = LoginUseCase(PartnerRepositoryImpl()),
    private val checkServerStatusUseCase: CheckServerStatusUseCase = CheckServerStatusUseCase(PartnerRepositoryImpl())
) : ViewModel() {

    sealed interface LoginUiState {
        object Idle : LoginUiState
        object Loading : LoginUiState
        object Success : LoginUiState
        data class Error(val message: String) : LoginUiState
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _pingState = MutableStateFlow<String?>(null)
    val pingState: StateFlow<String?> = _pingState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = loginUseCase(LoginRequest(email = email.trim(), password = password))
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        SessionManager.saveSession(
                            token = body.accessToken,
                            id = body.id,
                            name = body.name,
                            email = body.email,
                            lang = body.language
                        )
                    }
                    _uiState.value = LoginUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = LoginUiState.Error(parseError(response.code(), errorBody))
                }
            } catch (ex: Exception) {
                _uiState.value = LoginUiState.Error(parseConnectionError(ex))
            }
        }
    }

    fun checkAvailability() {
        viewModelScope.launch {
            try {
                val response = checkServerStatusUseCase()
                if (response.isSuccessful) {
                    _pingState.value = "Server Online: ${response.body()?.status}"
                } else {
                    _pingState.value = "Server unreachable (Code: ${response.code()})"
                }
            } catch (e: Exception) {
                _pingState.value = "Connection error: ${e.localizedMessage}"
            }
        }
    }

    fun clearPingState() {
        _pingState.value = null
    }

    private fun parseError(code: Int, body: String?): String {
        val backendMessage = runCatching {
            if (body.isNullOrBlank()) "" else JSONObject(body).optString("error", "")
        }.getOrDefault("")
        return backendMessage.ifBlank {
            if (code == 401) "Invalid credentials." else "Server error ($code). Please try again."
        }
    }

    private fun parseConnectionError(exception: Exception): String = when {
        exception.message?.contains("failed to connect") == true ->
            "Connection failed. Is the backend running?"
        exception.message?.contains("timeout") == true ->
            "Connection timeout. Check your network."
        else -> "Error: ${exception.localizedMessage ?: "Unknown error"}"
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
