package com.sismptm.client.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // Ping Endpoint
    @GET("ping")
    suspend fun ping(): Response<PingResponse>
    
    // Ejemplo de endpoint de registro
    @POST("auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<Unit>

    // Ejemplo de endpoint de login
    @POST("auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<AuthResponse>
}

// Modelos de datos para las peticiones (DTOs)
data class PingResponse(
    val status: String,
    val timestamp: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val creationDate: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val userName: String
)
