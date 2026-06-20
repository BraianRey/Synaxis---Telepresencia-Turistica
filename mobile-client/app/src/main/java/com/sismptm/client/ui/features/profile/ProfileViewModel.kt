package com.sismptm.client.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.domain.model.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.sismptm.client.core.events.ProfileEvents
import kotlinx.coroutines.flow.collect
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.ResponseBody

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _profilePictureBitmap = MutableStateFlow<Bitmap?>(null)
    val profilePictureBitmap: StateFlow<Bitmap?> = _profilePictureBitmap.asStateFlow()

    init {
        loadProfile()
        loadProfilePicture()

        // Listen for profile-picture-updated events and reload when they occur
        viewModelScope.launch {
            try {
                ProfileEvents.profilePictureUpdated.collect {
                    // force quick refresh
                    _profilePictureBitmap.value = null
                    loadProfilePicture()
                }
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val profile = RetrofitClient.apiService.getMyProfile()
                val profileState = ProfileUiState(
                    name = profile.name.orEmpty(),
                    email = profile.email.orEmpty(),
                    role = profile.role.orEmpty(),
                    language = profile.language?.ifBlank { SessionManager.language } ?: SessionManager.language,
                    status = profile.status.orEmpty(),
                    isLoading = false,
                    error = null
                )

                _uiState.value = profileState

                SessionManager.saveSession(
                    token = SessionManager.accessToken,
                    id = profile.id?.toLong() ?: SessionManager.userId,
                    name = profile.name.orEmpty(),
                    email = profile.email.orEmpty(),
                    role = profile.role.orEmpty(),
                    lang = profile.language ?: SessionManager.language
                )
            } catch (ex: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (ex.code() == 401) {
                        "Sesión expirada. Inicia sesión nuevamente."
                    } else {
                        "No se pudo cargar el perfil (Código ${ex.code()})."
                    }
                )
            } catch (ex: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error de conexión. Revisa tu red e intenta de nuevo."
                )
            } catch (ex: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ex.localizedMessage ?: "Error inesperado al cargar el perfil."
                )
            }
        }
    }

    fun loadProfilePicture() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.downloadProfilePicture()
                if (response.isSuccessful) {
                    val body: ResponseBody? = response.body()
                    val bytes = body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        _profilePictureBitmap.value = bitmap
                    } else {
                        _profilePictureBitmap.value = null
                    }
                } else {
                    _profilePictureBitmap.value = null
                }
            } catch (ex: Exception) {
                _profilePictureBitmap.value = null
            }
        }
    }
}
