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
        // Paso 1: mostrar nombre inmediatamente desde sesión local
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

        // Paso 2: refrescar desde backend en segundo plano
        val apiService = RetrofitClient.apiService
        viewModelScope.launch {
            try {
                val profile = apiService.getMyProfile()
                val fullName = "${profile.firstName} ${profile.lastName}".trim()
                if (fullName.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(userName = fullName)
                }
            } catch (e: Exception) {
                // Mantiene el nombre local, no muestra error
            }
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            try {
                // Cargar perfil del usuario
                val userProfile = RetrofitClient.apiService.getUserProfile()

                // Datos placeholder para destinations
                val destinations = listOf(
                    Destination(
                        id = 1,
                        city = "Popayán",
                        country = "Colombia",
                        placeName = "Puente del Humilladero",
                        activePartners = 3
                    ),
                    Destination(
                        id = 2,
                        city = "París",
                        country = "Francia",
                        placeName = "Eiffel Tower",
                        activePartners = 1
                    ),
                    Destination(
                        id = 3,
                        city = "Kyoto",
                        country = "Japón",
                        placeName = "Fushimi Inari",
                        activePartners = 2
                    )
                )

                // Datos placeholder para mapPins
                val mapPins = listOf(
                    MapPin(
                        id = 1,
                        city = "Popayán",
                        activeGuides = 3,
                        lat = 2.44f,
                        lng = -76.61f
                    ),
                    MapPin(
                        id = 2,
                        city = "París",
                        activeGuides = 1,
                        lat = 48.86f,
                        lng = 2.35f
                    ),
                    MapPin(
                        id = 3,
                        city = "Kyoto",
                        activeGuides = 2,
                        lat = 35.01f,
                        lng = 135.77f
                    )
                )

                // Actualizar estado
                _uiState.value = HomeUiState(
                    userName = "${userProfile.firstName} ${userProfile.lastName}",
                    destinations = destinations,
                    mapPins = mapPins,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                // En caso de error, actualizar el estado con datos placeholder
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}
