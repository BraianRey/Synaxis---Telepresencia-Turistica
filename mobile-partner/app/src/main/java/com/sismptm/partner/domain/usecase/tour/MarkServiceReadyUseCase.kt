package com.sismptm.partner.domain.usecase.tour

import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.data.remote.api.dto.ServiceResponse
import retrofit2.Response

/**
 * Use case to transition the service to STARTED.
 */
class MarkServiceReadyUseCase {
    suspend operator fun invoke(serviceId: Long): Response<ServiceResponse> =
        RetrofitClient.apiService.readyService(serviceId)
}
