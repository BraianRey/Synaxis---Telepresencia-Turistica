package com.sismptm.client.data.remote

object TokenManager {
    private var accessToken: String = ""
    private var userName: String = ""
    private var userId: Long = -1L

    fun saveSession(token: String, name: String, id: Long) {
        accessToken = token
        userName = name
        userId = id
    }

    fun getAccessToken(): String = accessToken
    fun getUserName(): String = userName
    fun getUserId(): Long = userId
    fun isLoggedIn(): Boolean = accessToken.isNotBlank()

    fun clearSession() {
        accessToken = ""
        userName = ""
        userId = -1L
    }
}
