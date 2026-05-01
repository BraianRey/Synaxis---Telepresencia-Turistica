package com.sismptm.client.data.remote.api.dto

/**
 * Data classes for service and tour-related requests and responses.
 */

data class CreateServiceRequest(
    val longitude: Double,
    val latitude: Double,
    val startLocationDescription: String?
)

data class ServiceResponse(
    val serviceId: Long,
    val clientId: Long,
    val clientName: String? = null,
    val partnerId: Long?,
    val longitude: Double,
    val latitude: Double,
    val startLocationDescription: String?,
    val agreedHours: Int,
    val hourlyRate: Double,
    val status: String,
    val requestedAt: String?,
    val acceptedAt: String?,
    val startedAt: String?,
    val endedAt: String?
)
