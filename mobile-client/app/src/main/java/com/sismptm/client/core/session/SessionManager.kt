package com.sismptm.client.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton object that manages the user session, including authentication tokens 
 * and user-specific information. This resides in memory during the app's lifecycle.
 */
object SessionManager {
    var accessToken: String = ""
        private set
    
    var userId: Long = -1L
        private set
        
    var userName: String = ""
        private set
        
    var userEmail: String = ""
        private set
        
    var userRole: String = ""
        private set

    private val _languageFlow = MutableStateFlow("en")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    var language: String
        get() = _languageFlow.value
        private set(value) { _languageFlow.value = value }

    /**
     * Checks if a user is currently logged in based on the presence of an access token.
     */
    fun isLoggedIn(): Boolean = accessToken.isNotBlank()

    /**
     * Saves the user session data.
     */
    fun saveSession(token: String, id: Long, name: String, email: String, role: String, lang: String? = "en") {
        accessToken = token
        userId = id
        userName = name
        userEmail = email
        userRole = role
        language = lang ?: "en"
    }

    /**
     * Clears all session data, effectively logging the user out.
     */
    fun clearSession() {
        accessToken = ""
        userId = -1L
        userName = ""
        userEmail = ""
        userRole = ""
        language = "en"
    }
}
