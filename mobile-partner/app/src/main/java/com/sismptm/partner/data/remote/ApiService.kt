package com.sismptm.partner.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("api/availability/ping") suspend fun ping(): Response<PingResponse>

    @POST("auth/partner/register")
    suspend fun registerPartner(@Body registerRequest: RegisterRequest): Response<Unit>

    @POST("auth/partner/login")
    suspend fun loginPartner(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("/location/update")
    suspend fun updateLocation(@Body locationRequest: LocationUpdateRequest): Response<Unit>
}

data class LocationUpdateRequest(val latitude: Double, val longitude: Double)

data class PingResponse(val status: String, val timestamp: String)

data class RegisterRequest(
        val name: String,
        val email: String,
        val password: String,
        val creationDate: String
)

data class LoginRequest(val email: String, val password: String)

data class AuthResponse(val token: String, val partnerId: String, val partnerName: String)
