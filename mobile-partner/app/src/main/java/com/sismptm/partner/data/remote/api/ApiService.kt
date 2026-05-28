package com.sismptm.partner.data.remote.api

import com.sismptm.partner.data.remote.api.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import okhttp3.MultipartBody

/**
 * Service interface for backend API communication.
 */
interface ApiService {
    @GET("api/availability/ping")
    suspend fun availabilityPing(): Response<PingResponse>

    /** Uploads a profile picture and returns the stored path */
    @Multipart
    @POST("api/upload/profile-pic")
    suspend fun uploadProfilePicture(@Part file: MultipartBody.Part): Response<UploadResponse>

    @POST("api/partners/register")
    suspend fun registerPartner(@Body request: RegisterPartnerRequest): Response<RegisterPartnerResponse>

    @POST("api/auth/partner/login")
    suspend fun loginPartner(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/partners/location/update")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): Response<Unit>

    @GET("api/services/available")
    suspend fun getAvailableServices(): Response<List<ServiceResponse>>

    @GET("api/services/partner/{partnerId}")
    suspend fun getServicesByPartner(@Path("partnerId") partnerId: Long): Response<List<ServiceResponse>>

    @POST("api/services/{serviceId}/accept")
    suspend fun acceptService(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    @POST("api/services/{serviceId}/ready")
    suspend fun readyService(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    @POST("api/services/{serviceId}/start")
    suspend fun startService(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    @POST("api/services/{serviceId}/complete")
    suspend fun completeService(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    @POST("api/services/{serviceId}/cancel/by-partner")
    suspend fun cancelServiceByPartner(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    /** Retrieves the payment summary for a completed service */
    @GET("api/services/{serviceId}/payment")
    suspend fun getPaymentSummary(
        @Path("serviceId") serviceId: Long
    ): Response<PaymentSummaryResponse>

    @GET("api/services/{serviceId}")
    suspend fun getServiceById(@Path("serviceId") serviceId: Long): Response<ServiceResponse>

    /** Retrieves all ratings for a specific partner */
    @GET("api/ratings/partner/{partnerId}")
    suspend fun getRatingsByPartner(@Path("partnerId") partnerId: Long): Response<List<RatingResponse>>
}
