package com.sismptm.client.data.remote.api

import com.sismptm.client.data.remote.api.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import okhttp3.MultipartBody

/**
 * Interface defining the API endpoints for the application.
 */
interface ApiService {

    /** Uploads a profile picture and returns the stored path */
    @Multipart
    @POST("api/upload/profile-pic")
    suspend fun uploadProfilePicture(@Part file: MultipartBody.Part): Response<UploadResponse>

    /** Registers a new client */
    @POST("api/clients/register")
    suspend fun registerClient(@Body request: RegisterClientRequest): Response<RegisterClientResponse>

    /** Authenticates a client and returns session tokens */
    @POST("api/auth/client/login")
    suspend fun loginClient(@Body request: LoginRequest): Response<LoginResponse>

    /** Creates a new service request */
    @POST("api/services/create")
    suspend fun createService(@Body request: CreateServiceRequest): Response<ServiceResponse>

    /** Retrieves the authenticated user's profile */
    @GET("api/clients/profile")
    suspend fun getUserProfile(): UserProfileResponse

    /** Retrieves the current authenticated user's basic profile */
    @GET("api/users/me")
    suspend fun getMyProfile(): UserProfileResponse

    /** Retrieves all services associated with a specific client */
    @GET("api/services/client/{clientId}")
    suspend fun getServicesByClient(@Path("clientId") clientId: Long): Response<List<ServiceResponse>>

    /** Retrieves all active services associated with a specific client */
    @GET("api/services/client/{clientId}/active")
    suspend fun getActiveServicesByClient(@Path("clientId") clientId: Long): Response<List<ServiceResponse>>

    /** Retrieves detailed information for a specific service */
    @GET("api/services/{serviceId}")
    suspend fun getServiceById(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    /** Cancels an existing service request */
    @POST("api/services/{serviceId}/cancel")
    suspend fun cancelService(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    /** Retrieves the payment summary for a completed service */
    @GET("api/services/{serviceId}/payment")
    suspend fun getPaymentSummary(
        @Path("serviceId") serviceId: Long
    ): Response<PaymentSummaryResponse>

    /** Confirms the simulated payment for a completed service */
    @PATCH("api/services/{serviceId}/payment/confirm")
    suspend fun confirmPayment(
        @Path("serviceId") serviceId: Long
    ): Response<PaymentSummaryResponse>

    /** Completes an in-progress service session initiated by the client */
    @POST("api/services/{serviceId}/client-complete")
    suspend fun completeServiceByClient(
        @Path("serviceId") serviceId: Long
    ): Response<ServiceResponse>

    /** Creates a new rating for a completed service */
    @POST("api/ratings")
    suspend fun createRating(@Body request: RatingRequest): Response<RatingResponse>

    /** Retrieves the rating associated with a specific service */
    @GET("api/ratings/service/{serviceId}")
    suspend fun getRatingByService(@Path("serviceId") serviceId: Long): Response<RatingResponse>
}
