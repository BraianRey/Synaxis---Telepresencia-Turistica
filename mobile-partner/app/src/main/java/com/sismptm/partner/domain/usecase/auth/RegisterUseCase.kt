package com.sismptm.partner.domain.usecase.auth

import com.sismptm.partner.data.remote.api.dto.RegisterPartnerRequest
import com.sismptm.partner.data.remote.api.dto.RegisterPartnerResponse
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to handle partner registration.
 */
class RegisterUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(request: RegisterPartnerRequest): Response<RegisterPartnerResponse> = 
        repository.register(request)
}
