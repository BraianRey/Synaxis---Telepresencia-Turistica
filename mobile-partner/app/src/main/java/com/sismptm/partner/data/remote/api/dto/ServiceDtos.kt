package com.sismptm.partner.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for service and tour related operations.
 */
data class ServiceResponse(
    @SerializedName("serviceId") val serviceId: Long,
    @SerializedName("clientId") val clientId: Long,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("partnerId") val partnerId: Long?,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("startLocationDescription") val startLocationDescription: String?,
    @SerializedName("agreedHours") val agreedHours: Int,
    @SerializedName("hourlyRate") val hourlyRate: Double,
    @SerializedName("status") val status: String,
    @SerializedName("requestedAt") val requestedAt: String?,
    @SerializedName("acceptedAt") val acceptedAt: String?,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("endedAt") val endedAt: String?
)

data class LocationUpdateRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
