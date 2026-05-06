package com.sismptm.partner.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for service and tour related operations.
 */
data class ServiceResponse(
    @SerializedName("serviceId") val serviceId: Long,
    // Client information
    @SerializedName("clientId") val clientId: Long,
    @SerializedName("clientName") val clientName: String? = null,
    @SerializedName("clientEmail") val clientEmail: String? = null,
    @SerializedName("clientPicDirectory") val clientPicDirectory: String? = null,
    // Partner information
    @SerializedName("partnerId") val partnerId: Long?,
    @SerializedName("partnerName") val partnerName: String? = null,
    @SerializedName("partnerEmail") val partnerEmail: String? = null,
    @SerializedName("partnerPicDirectory") val partnerPicDirectory: String? = null,
    // Service details
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("startLocationDescription") val startLocationDescription: String?,
    @SerializedName("agreedHours") val agreedHours: Int,
    @SerializedName("hourlyRate") val hourlyRate: Double? = 15000.0,
    @SerializedName("status") val status: String,
    @SerializedName("requestedAt") val requestedAt: String?,
    @SerializedName("acceptedAt") val acceptedAt: String?,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("endedAt") val endedAt: String?
) {
    /**
     * Calculates the service duration in minutes.
     */
    fun getDurationMinutes(): Long? {
        if (startedAt == null || endedAt == null) return null
        return try {
            val start = java.time.Instant.parse(startedAt)
            val end = java.time.Instant.parse(endedAt)
            java.time.Duration.between(start, end).toMinutes()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the total cost based on duration and hourly rate.
     */
    fun getTotalCost(): Double? {
        val durationMinutes = getDurationMinutes()
        if (durationMinutes == null || hourlyRate == null) return null
        val hours = durationMinutes / 60.0
        return kotlin.math.round(hours * hourlyRate * 100.0) / 100.0
    }

    /**
     * Gets formatted duration string (e.g., "45 min" or "1h 30m").
     */
    fun getFormattedDuration(): String {
        val minutes = getDurationMinutes() ?: return "N/A"
        if (minutes < 60) return "$minutes min"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (remainingMinutes == 0L) "${hours}h" else "${hours}h ${remainingMinutes}m"
    }

    /**
     * Gets formatted cost string with currency.
     */
    fun getFormattedCost(): String {
        val cost = getTotalCost() ?: return "N/A"
        return String.format("$%,.0f COP", cost)
    }
}

data class LocationUpdateRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
