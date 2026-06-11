package com.sismptm.partner.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.core.events.ProfileEvents
import com.sismptm.partner.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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
        }
    }
}
