package com.sismptm.client.data.remote.api.dto

/**
 * Data classes for service and tour-related requests and responses.
 */

data class CreateServiceRequest(
    val longitude: Double,
    val latitude: Double,
    val startLocationDescription: String?,
    val scheduledAt: String? = null
)

/**
 * Service response with comprehensive information including client and partner details.
 */
data class ServiceResponse(
    val serviceId: Long,
    // Client information
    val clientName: String? = null,
    val clientEmail: String? = null,
    // Partner information
    val partnerName: String? = null,
    val partnerEmail: String? = null,
    // Service details
    val startLocationDescription: String?,
    val agreedHours: Int? = null,
    val hourlyRate: Double? = null,
    val status: String,
    val startedAt: String?,
    val endedAt: String?,
    val locationReferenceImageUrl: String? = null,
    val scheduledAt: String? = null
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

data class PaymentSummaryResponse(
    val serviceId: Long,
    val actualDurationMin: Int,
    val billedHours: Double,
    val totalAmount: Double,
    val hourlyRate: Double,
    val calculatedAt: String,
    val confirmed: Boolean
)
