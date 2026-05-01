package com.sismptm.partner.domain.usecase.tour

import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to notify the server that the partner is ready to start the tour.
 */
class MarkServiceReadyUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(serviceId: Long): Response<Unit> = 
        repository.markServiceReady(serviceId)
}
