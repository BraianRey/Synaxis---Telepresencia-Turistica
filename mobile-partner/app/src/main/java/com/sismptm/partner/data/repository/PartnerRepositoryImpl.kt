package com.sismptm.partner.data.repository

import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.data.remote.api.dto.*
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Implementation of PartnerRepository using Retrofit for remote data.
 */
class PartnerRepositoryImpl : PartnerRepository {
    
    private val api = RetrofitClient.apiService

    override suspend fun ping(): Response<PingResponse> = api.availabilityPing()

    override suspend fun login(request: LoginRequest): Response<LoginResponse> = 
        api.loginPartner(request)

    override suspend fun register(request: RegisterPartnerRequest): Response<RegisterPartnerResponse> = 
        api.registerPartner(request)

    override suspend fun updateLocation(request: LocationUpdateRequest): Response<Unit> = 
        api.updateLocation(request)

    override suspend fun getAvailableServices(): Response<List<ServiceResponse>> = 
        api.getAvailableServices()

    override suspend fun getPartnerServices(partnerId: Long): Response<List<ServiceResponse>> = 
        api.getServicesByPartner(partnerId)

    override suspend fun acceptService(serviceId: Long): Response<ServiceResponse> = 
        api.acceptService(serviceId)

    override suspend fun markServiceReady(serviceId: Long): Response<Unit> = 
        api.markServiceAsReady(serviceId)
}
