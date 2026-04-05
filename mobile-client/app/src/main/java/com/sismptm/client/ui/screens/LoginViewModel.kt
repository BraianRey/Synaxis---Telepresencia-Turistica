package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.LoginRequest
import com.sismptm.client.data.remote.RetrofitClient
import com.sismptm.client.utils.SessionManager
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = RetrofitClient.apiService.loginClient(
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
                            role = body.role
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

    private fun parseConnectionError(exception: Exception): String = when {
        exception.message?.contains("failed to connect") == true ->
            "Connection failed. Is the backend running?"
        exception.message?.contains("timeout") == true ->
            "Connection timeout. Check your network."
        else -> "Error: ${exception.localizedMessage ?: "Unknown error"}"
    }
}
