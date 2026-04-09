package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.sismptm.client.data.remote.RetrofitClient
import com.sismptm.client.data.remote.TokenManager

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        // Step 1: Display name immediately from local session
        val localName = TokenManager.getUserName()
        _uiState.value = _uiState.value.copy(
            userName = if (localName.isNotBlank()) localName else "Viajero",
            isLoading = false,
            destinations = listOf(
                Destination(1, "Popayán", "Colombia", "Puente del Humilladero", 3),
                Destination(2, "París", "Francia", "Eiffel Tower", 1),
                Destination(3, "Kyoto", "Japón", "Fushimi Inari", 2)
            ),
            mapPins = listOf(
                MapPin(1, "Popayán", 3, 0.3f, 0.6f),
                MapPin(2, "París", 1, 0.7f, 0.3f),
                MapPin(3, "Kyoto", 2, 0.6f, 0.7f)
            )
        )

        // Step 2: Refresh from backend in background
        val apiService = RetrofitClient.apiService
        viewModelScope.launch {
            try {
                val profile = apiService.getMyProfile()
               val fullName = profile.name.trim()
                if (fullName.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(userName = fullName)
                }
            } catch (e: Exception) {
                // Keep local name, don't show error
            }
        }
    }
}
