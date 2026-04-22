package com.sismptm.client.utils

/** In-memory session store. Survives within the app process but clears on re-launch. */
object SessionManager {
    var accessToken: String = ""
    var clientId: Long = 0L
    var clientName: String = ""
    var userRole: String = ""
    var clientEmail: String = ""

    fun isLoggedIn(): Boolean = accessToken.isNotEmpty()

    fun saveSession(token: String, id: Long, name: String, email: String, role: String) {
        accessToken = token
        clientId = id
        clientName = name
        userRole = role
        clientEmail = email
    }

    // Backward-compatible overload for callers that still do not provide role.
    fun saveSession(token: String, id: Long, name: String, email: String) {
        saveSession(token = token, id = id, name = name, email = email, role = "")
    }

    fun clearSession() {
        accessToken = ""
        clientId = 0L
        clientName = ""
        userRole = ""
        clientEmail = ""
    }
}
