package com.sismptm.partner.domain.usecase.auth

import com.sismptm.partner.data.remote.api.dto.PingResponse
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to check if the backend server is reachable.
 */
class CheckServerStatusUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(): Response<PingResponse> = repository.ping()
}
