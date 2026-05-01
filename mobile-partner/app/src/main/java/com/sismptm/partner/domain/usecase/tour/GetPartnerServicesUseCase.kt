package com.sismptm.partner.domain.usecase.tour

import com.sismptm.partner.data.remote.api.dto.ServiceResponse
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to retrieve all services associated with a specific partner.
 */
class GetPartnerServicesUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(partnerId: Long): Response<List<ServiceResponse>> = 
        repository.getPartnerServices(partnerId)
}
