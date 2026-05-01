package com.sismptm.partner.domain.usecase.location

import com.sismptm.partner.data.remote.api.dto.LocationUpdateRequest
import com.sismptm.partner.domain.repository.PartnerRepository
import retrofit2.Response

/**
 * Use case to update the partner's current GPS location on the server.
 */
class UpdateLocationUseCase(private val repository: PartnerRepository) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Response<Unit> = 
        repository.updateLocation(LocationUpdateRequest(latitude, longitude))
}
