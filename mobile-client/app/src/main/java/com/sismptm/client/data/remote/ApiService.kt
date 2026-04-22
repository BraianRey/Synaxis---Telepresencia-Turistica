package com.sismptm.client.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interface defining the API endpoints for authentication, registration, and WebRTC signaling.
 */
interface ApiService {

    /** 
     * Registers a new client in the system.
     * POST /api/clients/register/client 
     */
    @POST("api/clients/register/client")
    suspend fun registerClient(@Body request: RegisterClientRequest): Response<RegisterClientResponse>

    /** 
     * Authenticates a client and returns session tokens.
     * POST /api/auth/client/login 
     */
    @POST("api/auth/client/login")
    suspend fun loginClient(@Body request: LoginRequest): Response<LoginResponse>

    /** 
     * Fetches the current user profile data.
     * GET /api/clients/profile 
     */
    @GET("api/clients/profile")
    suspend fun getUserProfile(): UserProfileResponse

    /** 
     * Fetches profile data for the authenticated user.
     * GET /api/users/me 
     */
    @GET("api/users/me")
    suspend fun getMyProfile(): UserProfileResponse

    /* --- WebRTC Signaling Endpoints --- */

    /**
     * Sends an ICE Candidate to the peer via the signaling server.
     */
    @POST("api/webrtc/ice-candidate")
    suspend fun sendIceCandidate(@Body candidate: IceCandidateModel): Response<Unit>

    /**
     * Sends an SDP Offer/Answer to the peer via the signaling server.
     */
    @POST("api/webrtc/sdp")
    suspend fun sendSdp(@Body sdp: SdpModel): Response<Unit>
}

/* --- WebRTC Signaling Data Models --- */

/**
 * Data model for ICE Candidate exchange.
 */
data class IceCandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String,
    val targetUserId: String
)

/**
 * Data model for SDP (Offer/Answer) exchange.
 */
data class SdpModel(
    val type: String, // "OFFER" or "ANSWER"
    val sdp: String,
    val targetUserId: String
)

/* --- Auth & Profile Data Models --- */

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

// -- Service DTOs --------------------------------------------------------------
data class CreateServiceRequest(
    val longitude: Double,
    val latitude: Double,
    val startLocationDescription: String?,
    val agreedHours: Int,
    val hourlyRate: Double
)

data class ServiceResponse(
    val serviceId: Long,
    val clientId: Long,
    val clientName: String? = null,
    val partnerId: Long?,
    val longitude: Double,
    val latitude: Double,
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
