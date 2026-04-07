package com.sismptm.client.utils

/** In-memory session store. Survives within the app process but clears on re-launch. */
object SessionManager {
    var accessToken: String = ""
    var clientId: Long = 0L
    var clientName: String = ""
    var clientEmail: String = ""
    var userRole: String = ""

    fun isLoggedIn(): Boolean = accessToken.isNotEmpty()

    fun saveSession(token: String, id: Long, name: String, email: String, role: String) {
        accessToken = token
        clientId = id
        clientName = name
        clientEmail = email
        userRole = role
    }

    fun clearSession() {
        accessToken = ""
        clientId = 0L
        clientName = ""
        clientEmail = ""
        userRole = ""
    }
}
