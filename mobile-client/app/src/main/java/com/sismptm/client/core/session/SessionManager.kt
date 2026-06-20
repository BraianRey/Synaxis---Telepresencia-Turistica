package com.sismptm.client.core.session

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton object that manages the user session, including authentication tokens 
 * and user-specific information. This resides in memory during the app's lifecycle.
 */
object SessionManager {
    private const val PREFS_NAME = "client_session_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_LANGUAGE = "language"
    

    private var preferences: SharedPreferences? = null

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

    // picDirectory removed: images are loaded from backend binary storage

    private val _languageFlow = MutableStateFlow("en")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    var language: String
        get() = _languageFlow.value
        private set(value) { _languageFlow.value = value }

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSession()
    }

    private fun loadSession() {
        preferences?.let { prefs ->
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty()
            userId = prefs.getLong(KEY_USER_ID, -1L)
            userName = prefs.getString(KEY_USER_NAME, "").orEmpty()
            userEmail = prefs.getString(KEY_USER_EMAIL, "").orEmpty()
            userRole = prefs.getString(KEY_USER_ROLE, "").orEmpty()
            language = prefs.getString(KEY_LANGUAGE, "en").orEmpty()
            
        }
    }

    private fun persistSession() {
        preferences?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_ROLE, userRole)
            putString(KEY_LANGUAGE, language)
            apply()
        }
    }

    fun updateLanguage(lang: String) {
        language = lang
        persistSession()
    }

    // updatePicDirectory removed - profile images are stored server-side as bytes

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
        persistSession()
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
        
        preferences?.edit()?.clear()?.apply()
    }
}
