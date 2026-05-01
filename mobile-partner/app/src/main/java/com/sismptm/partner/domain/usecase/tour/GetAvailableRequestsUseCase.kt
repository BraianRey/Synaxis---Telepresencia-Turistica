package com.sismptm.partner.domain.usecase.tour

import com.sismptm.partner.data.remote.api.dto.ServiceResponse
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to retrieve all available (unassigned) tour requests.
 */
class GetAvailableRequestsUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(): Response<List<ServiceResponse>> = 
        repository.getAvailableServices()
}
