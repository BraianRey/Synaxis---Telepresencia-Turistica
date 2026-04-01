package com.sismptm.partner.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    /** GET /ping */
    @GET("ping")
    suspend fun ping(): Response<PingResponse>

    /** POST /api/partners/register/partner */
    @POST("api/partners/register/partner")
    suspend fun registerPartner(@Body request: RegisterPartnerRequest): Response<RegisterPartnerResponse>

    /** POST /api/auth/partner/login */
    @POST("api/auth/partner/login")
    suspend fun loginPartner(@Body request: LoginRequest): Response<LoginResponse>

    /** POST /api/partners/location/update */
    @POST("api/partners/location/update")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): Response<Unit>
}

data class PingResponse(
    val status: String
)

// ── Request DTO (espeja RegisterPartnerRequest del backend) ──────────────────
data class RegisterPartnerRequest(
    val email: String,
    val password: String,
    val name: String,
    val areaId: Int,
    val termsAccepted: Boolean,
    val language: String,        // "en" | "es"
    val picDirectory: String? = null
)

// ── Response DTO (espeja RegisterPartnerResponse del backend) ─────────────────
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
    val areaId: Int,
    val availabilityStatus: String
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

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double
)
