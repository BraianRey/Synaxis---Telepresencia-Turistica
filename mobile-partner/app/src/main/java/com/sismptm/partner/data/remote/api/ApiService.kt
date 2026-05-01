package com.sismptm.partner.data.remote.api

import com.sismptm.partner.data.remote.api.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Service interface for backend API communication.
 */
interface ApiService {
    @GET("api/availability/ping")
    suspend fun availabilityPing(): Response<PingResponse>

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
    suspend fun markServiceAsReady(@Path("serviceId") serviceId: Long): Response<Unit>
}
