package com.sismptm.partner.domain.repository

import com.sismptm.partner.data.remote.api.dto.*
import retrofit2.Response

/**
 * Repository interface for partner-related data operations.
 */
interface PartnerRepository {
    suspend fun ping(): Response<PingResponse>
    suspend fun login(request: LoginRequest): Response<LoginResponse>
    suspend fun register(request: RegisterPartnerRequest): Response<RegisterPartnerResponse>
    suspend fun updateLocation(request: LocationUpdateRequest): Response<Unit>
    suspend fun getAvailableServices(): Response<List<ServiceResponse>>
    suspend fun getPartnerServices(partnerId: Long): Response<List<ServiceResponse>>
    suspend fun acceptService(serviceId: Long): Response<ServiceResponse>
    suspend fun markServiceReady(serviceId: Long): Response<Unit>
}
