package com.sismptm.partner.utils

/** In-memory session store. Survives within the app process but clears on re-launch. */
object SessionManager {
    var accessToken: String = ""
    var partnerId: Long = 0L
    var partnerName: String = ""
    var partnerEmail: String = ""
    var language: String = "en"

    fun isLoggedIn(): Boolean = accessToken.isNotEmpty()

    fun saveSession(token: String, id: Long, name: String, email: String, lang: String? = "en") {
        accessToken = token
        partnerId = id
        partnerName = name
        partnerEmail = email
        language = lang ?: "en"
    }

    fun clearSession() {
        accessToken = ""
        partnerId = 0L
        partnerName = ""
        partnerEmail = ""
        language = "en"
    }
}
