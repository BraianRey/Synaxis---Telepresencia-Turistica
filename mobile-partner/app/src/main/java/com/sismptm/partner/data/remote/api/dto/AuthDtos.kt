package com.sismptm.partner.data.remote.api.dto

/**
 * Data transfer objects for authentication related operations.
 */
data class PingResponse(val status: String)

data class LoginRequest(val email: String, val password: String)

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

data class RegisterPartnerRequest(
    val email: String,
    val password: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val termsAccepted: Boolean,
    val language: String,
    val picDirectory: String? = null
)

data class RegisterPartnerResponse(
    val id: Long,
    val email: String,
    val name: String,
    val status: String,
    val language: String,
    val createdAt: String,
    val termsAccepted: Boolean,
    val picDirectory: String?,
    val role: String,
    val availabilityStatus: String
)
