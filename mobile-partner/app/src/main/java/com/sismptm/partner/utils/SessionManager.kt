package com.sismptm.partner.utils

/** In-memory session store. Survives within the app process but clears on re-launch. */
object SessionManager {
    var accessToken: String = ""
    var partnerId: Long = 0L
    var partnerName: String = ""
    var partnerEmail: String = ""
    /** Area ID selected by the partner (1=Popayán, 2=Cali, 3=Medellín, 4=Bogotá). */
    var areaId: Long = 0L

    fun isLoggedIn(): Boolean = accessToken.isNotEmpty()

    fun saveSession(token: String, id: Long, name: String, email: String, areaId: Long = 0L) {
        accessToken = token
        partnerId = id
        partnerName = name
        partnerEmail = email
        this.areaId = areaId
    }

    fun clearSession() {
        accessToken = ""
        partnerId = 0L
        partnerName = ""
        partnerEmail = ""
        areaId = 0L
    }
}

