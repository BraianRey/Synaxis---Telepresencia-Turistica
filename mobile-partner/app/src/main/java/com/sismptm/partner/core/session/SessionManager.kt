package com.sismptm.partner.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton managing the user session, providing a single source of truth for 
 * tokens, user identification, and preferences.
 */
object SessionManager {
    var accessToken: String = ""
    var partnerId: Long = 0L
    var partnerName: String = ""
    var partnerEmail: String = ""
    var partnerRole: String = ""
    var picDirectory: String? = null

    private val _languageFlow = MutableStateFlow("en")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    var language: String
        get() = _languageFlow.value
        private set(value) { _languageFlow.value = value }
        
    fun updateLanguage(lang: String) {
        language = lang
    }
    
    fun isLoggedIn(): Boolean = accessToken.isNotEmpty()

    fun saveSession(token: String, id: Long, name: String, email: String, lang: String? = "en", role: String = "", picDirectory: String? = null) {
        accessToken = token
        partnerId = id
        partnerName = name
        partnerEmail = email
        partnerRole = role
        language = lang ?: "en"
        this.picDirectory = picDirectory
    }

    fun clearSession() {
        accessToken = ""
        partnerId = 0L
        partnerName = ""
        partnerEmail = ""
        partnerRole = ""
        language = "en"
        picDirectory = null
    }
}
