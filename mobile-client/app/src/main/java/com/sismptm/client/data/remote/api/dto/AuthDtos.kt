package com.sismptm.client.data.remote.api.dto

/**
 * Data classes for authentication-related requests and responses.
 */

data class LoginRequest(
    val email: String, 
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    val language: String? = "en"
)

data class RegisterClientRequest(
    val email: String,
    val password: String,
    val name: String,
    val termsAccepted: Boolean,
    val language: String,
    val picDirectory: String? = null
)

data class RegisterClientResponse(
    val id: Long,
    val email: String,
    val name: String,
    val status: String,
    val language: String,
    val createdAt: String,
    val termsAccepted: Boolean,
    val picDirectory: String?,
    val role: String
)

data class UserProfileResponse(
    val id: Int,
    val name: String,
    val email: String,
    val status: String,
    val language: String,
    val role: String,
    val picDirectory: String?
)
