package com.sismptm.partner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.data.remote.LoginRequest
import com.sismptm.partner.data.remote.RetrofitClient
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
                val response = RetrofitClient.apiService.loginPartner(
                    LoginRequest(email = email.trim(), password = password)
                )
                if (response.isSuccessful) {
                    _uiState.value = LoginUiState.Success
                } else {
                    _uiState.value = LoginUiState.Error(parseError(response.code(), response.errorBody()?.string()))
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
            if (body.isNullOrBlank()) null else JSONObject(body).optString("error", null)
        }.getOrNull()

        if (!backendMessage.isNullOrBlank()) {
            return backendMessage
        }

        return if (code == 401) {
            "Las credenciales no son correctas."
        } else {
            "Error del servidor. Intenta nuevamente."
        }
    }

    private fun parseConnectionError(exception: Exception): String {
        return when {
            exception.message?.contains("failed to connect") == true -> {
                "No se pudo conectar. Asegúrate de que el backend está corriendo en http://10.0.2.2:8080"
            }
            exception.message?.contains("timeout") == true -> {
                "Tiempo de conexión agotado. Verifica tu red y el estado del backend."
            }
            exception.message?.contains("Connection refused") == true -> {
                "Backend rechazó la conexión. ¿Está corriendo?"
            }
            else -> {
                "Error de conexión: ${exception.localizedMessage ?: "Error desconocido"}"
            }
        }
    }
}


