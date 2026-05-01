package com.sismptm.partner.domain.usecase.auth

import com.sismptm.partner.data.remote.api.dto.LoginRequest
import com.sismptm.partner.data.remote.api.dto.LoginResponse
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to handle user login.
 */
class LoginUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(request: LoginRequest): Response<LoginResponse> = 
        repository.login(request)
}
