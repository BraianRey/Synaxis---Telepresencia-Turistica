package com.sismptm.partner.ui.features.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.core.events.ProfileEvents
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _profilePictureBitmap = MutableStateFlow<Bitmap?>(null)
    val profilePictureBitmap: StateFlow<Bitmap?> = _profilePictureBitmap.asStateFlow()

    private val _profilePictureDirectory = MutableStateFlow<String?>(SessionManager.picDirectory)
    val profilePictureDirectory: StateFlow<String?> = _profilePictureDirectory.asStateFlow()

    init {
        loadProfile()
        loadProfilePicture()

        viewModelScope.launch {
            try {
                ProfileEvents.profilePictureUpdated.collect {
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

            _uiState.value = ProfileUiState(
                name = SessionManager.partnerName,
                email = SessionManager.partnerEmail,
                role = SessionManager.partnerRole,
                language = SessionManager.language,
                status = "",
                isLoading = false,
                error = null
            )
        }
    }

    fun loadProfilePicture() {
        viewModelScope.launch {
            _profilePictureDirectory.value = null
            _profilePictureDirectory.value = SessionManager.picDirectory

            _profilePictureBitmap.value = null
            try {
                val response = RetrofitClient.apiService.downloadProfilePicture()
                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        _profilePictureBitmap.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
