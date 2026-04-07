package com.sismptm.client.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    /** POST /api/clients/register */
    @POST("api/clients/register")
    suspend fun registerClient(@Body request: RegisterClientRequest): Response<RegisterClientResponse>

    /** POST /api/auth/client/login */
    @POST("api/auth/client/login")
    suspend fun loginClient(@Body request: LoginRequest): Response<LoginResponse>

    /** POST /api/services/create  - requires CLIENT token */
    @POST("api/services/create")
    suspend fun createService(@Body request: CreateServiceRequest): Response<ServiceResponse>

    /** GET /api/clients/profile */
    @GET("api/clients/profile")
    suspend fun getUserProfile(): UserProfileResponse

    /** GET /api/users/me */
    @GET("api/users/me")
    suspend fun getMyProfile(): UserProfileResponse

    /** GET /api/services/client/{clientId} - requires CLIENT token */
    @GET("api/services/client/{clientId}")
    suspend fun getServicesByClient(@Path("clientId") clientId: Long): Response<List<ServiceResponse>>

    /** GET /api/services/{serviceId} - requires CLIENT/PARTNER token */
    @GET("api/services/{serviceId}")
    suspend fun getServiceById(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    /** POST /api/services/{serviceId}/cancel - requires CLIENT token */
    @POST("api/services/{serviceId}/cancel")
    suspend fun cancelService(@Path("serviceId") serviceId: Long): Response<ServiceResponse>
}

// -- Auth DTOs -----------------------------------------------------------------
data class LoginRequest(val email: String, val password: String)

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

// Request DTO (mirrors RegisterClientRequest from backend)
data class RegisterClientRequest(
    val email: String,
    val password: String,
    val name: String,
    val termsAccepted: Boolean,
    val language: String,
    val picDirectory: String? = null
)

// Response DTO (mirrors RegisterClientResponse from backend)
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

// -- Service DTOs --------------------------------------------------------------
data class CreateServiceRequest(
    val areaId: Long,
    val startLocationDescription: String?,
    val agreedHours: Int,
    val hourlyRate: Double
)

data class ServiceResponse(
    val serviceId: Long,
    val clientId: Long,
    val clientName: String? = null,
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

data class UserProfileResponse(
    val id: Int,
    val name: String,
    val email: String,
    val status: String,
    val language: String,
    val role: String,
    val picDirectory: String?
)
