package com.sismptm.client.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    /** POST /api/clients/register/client */
    @POST("api/clients/register/client")
    suspend fun registerClient(@Body request: RegisterClientRequest): Response<RegisterClientResponse>

    /** POST /api/auth/client/login */
    @POST("api/auth/client/login")
    suspend fun loginClient(@Body request: LoginRequest): Response<LoginResponse>

    /** GET /api/clients/profile */
    @GET("api/clients/profile")
    suspend fun getUserProfile(): UserProfileResponse

    /** GET /api/users/me */
    @GET("api/users/me")
    suspend fun getMyProfile(): UserProfileResponse
}

// ── Request DTO (espeja RegisterClientRequest del backend) ──────────────────
data class RegisterClientRequest(
    val email: String,
    val password: String,
    val name: String,
    val termsAccepted: Boolean,
    val language: String,        // "en" | "es"
    val picDirectory: String? = null
)

// ── Response DTO (espeja RegisterClientResponse del backend) ─────────────────
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
    val role: String
)

data class UserProfileResponse(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String
)
