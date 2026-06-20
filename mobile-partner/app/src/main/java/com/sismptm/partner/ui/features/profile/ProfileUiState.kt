package com.sismptm.partner.ui.features.profile

/**
 * UI state representing the authenticated partner's profile.
 */
data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val language: String = "en",
    val status: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
