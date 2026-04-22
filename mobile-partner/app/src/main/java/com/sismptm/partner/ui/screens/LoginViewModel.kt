package com.sismptm.partner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.data.remote.LoginRequest
import com.sismptm.partner.data.remote.RetrofitClient
import com.sismptm.partner.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginViewModel : ViewModel() {

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
                val response = RetrofitClient.apiService.loginPartner(
                    LoginRequest(email = email.trim(), password = password)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
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
                    _uiState.value = LoginUiState.Error(
                        parseError(response.code(), response.errorBody()?.string())
                    )
                }
            } catch (ex: Exception) {
                _uiState.value = LoginUiState.Error(parseConnectionError(ex))
            }
        }
    }

    fun checkAvailability() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.availabilityPing()
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

    private fun parseConnectionError(exception: Exception): String = when {
        exception.message?.contains("failed to connect") == true ->
            "Connection failed. Is the backend running?"
        exception.message?.contains("timeout") == true ->
            "Connection timeout. Check your network."
        else -> "Error: ${exception.localizedMessage ?: "Unknown error"}"
    }
}
