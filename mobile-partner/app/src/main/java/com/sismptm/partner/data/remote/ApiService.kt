package com.sismptm.partner.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    /** GET /ping */
    @GET("ping")
    suspend fun ping(): Response<PingResponse>

    /** POST /api/partners/register */
    @POST("api/partners/register")
    suspend fun registerPartner(@Body request: RegisterPartnerRequest): Response<RegisterPartnerResponse>

    /** POST /api/auth/partner/login */
    @POST("api/auth/partner/login")
    suspend fun loginPartner(@Body request: LoginRequest): Response<LoginResponse>

    /** POST /api/partners/location/update  — requires PARTNER token */
    @POST("api/partners/location/update")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): Response<Unit>

    /** GET /api/services/available/{areaId}  — requires PARTNER token */
    @GET("api/services/available/{areaId}")
    suspend fun getServicesAvailableByAreaId(@Path("areaId") areaId: Long): Response<List<ServiceResponse>>

    /** POST /api/services/{serviceId}/accept  — requires PARTNER token */
    @POST("api/services/{serviceId}/accept")
    suspend fun acceptService(@Path("serviceId") serviceId: Long): Response<ServiceResponse>
}

// ── Auth DTOs ─────────────────────────────────────────────────────────────────
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
    val areaId: Long? = null
)

// ── Register DTOs ─────────────────────────────────────────────────────────────
data class RegisterPartnerRequest(
    val email: String,
    val password: String,
    val name: String,
    val areaId: Int,
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
    val areaId: Int,
    val availabilityStatus: String
)

// ── Location ──────────────────────────────────────────────────────────────────
data class LocationUpdateRequest(val latitude: Double, val longitude: Double)

// ── Service DTOs ──────────────────────────────────────────────────────────────
data class ServiceResponse(
    val serviceId: Long,
    val clientId: Long,
    val clientName: String,
    val partnerId: Long?,
    val areaId: Long,
    val startLocationDescription: String?,
    val agreedHours: Int,
    val hourlyRate: Double,
    val status: String,
    val requestedAt: String?,
    val acceptedAt: String?,
    val startedAt: String?,
    val endedAt: String?
)

