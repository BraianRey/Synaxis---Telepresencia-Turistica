package com.sismptm.partner.utils

/** In-memory session store. Survives within the app process but clears on re-launch. */
object SessionManager {
    var accessToken: String = ""
    var partnerId: Long = 0L
    var partnerName: String = ""
    var partnerEmail: String = ""

    fun isLoggedIn(): Boolean = accessToken.isNotEmpty()

    fun saveSession(token: String, id: Long, name: String, email: String) {
        accessToken = token
        partnerId = id
        partnerName = name
        partnerEmail = email
    }

    fun clearSession() {
        accessToken = ""
        partnerId = 0L
        partnerName = ""
        partnerEmail = ""
    }
}

