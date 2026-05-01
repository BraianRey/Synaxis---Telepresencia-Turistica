package com.sismptm.partner.domain.usecase.tour

import com.sismptm.partner.data.remote.api.dto.ServiceResponse
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to accept an available tour request.
 */
class AcceptTourUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(serviceId: Long): Response<ServiceResponse> = 
        repository.acceptService(serviceId)
}
