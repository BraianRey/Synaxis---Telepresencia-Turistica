package com.sismptm.partner.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for service and tour related operations.
 */
data class ServiceResponse(
    @SerializedName("serviceId") val serviceId: Long,
    // Client information
    @SerializedName("clientName") val clientName: String? = null,
    @SerializedName("clientEmail") val clientEmail: String? = null,
    @SerializedName("clientPicDirectory") val clientPicDirectory: String? = null,
    // Partner information
    @SerializedName("partnerName") val partnerName: String? = null,
    @SerializedName("partnerEmail") val partnerEmail: String? = null,
    // Service details
    @SerializedName("startLocationDescription") val startLocationDescription: String?,
    @SerializedName("agreedHours") val agreedHours: Int? = null,
    @SerializedName("hourlyRate") val hourlyRate: Double? = null,
    @SerializedName("status") val status: String,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("endedAt") val endedAt: String?,
    @SerializedName("locationReferenceImageUrl") val locationReferenceImageUrl: String? = null,
    @SerializedName("scheduled") val scheduled: Boolean? = null,
    @SerializedName("scheduledAt") val scheduledAt: String? = null
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
     * Estimates total: minimum 30 min at 5 USD, then billed per minute.
     */
    fun getTotalCost(): Double? {
        val durationMinutes = getDurationMinutes() ?: return null
        return PaymentPricing.estimateTotal(durationMinutes)
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
        return String.format("$%.2f USD", cost)
    }
}

object PaymentPricing {
    const val MIN_BILLING_MINUTES = 30
    const val MIN_PACKAGE_PRICE_USD = 5.0
    const val TIER1_RATE = 0.1667
    const val TIER2_RATE = 0.1500
    const val TIER3_RATE = 0.1333
    const val TIER4_RATE = 0.1167

    fun billedMinutes(actualDurationMin: Long): Long =
        maxOf(actualDurationMin, MIN_BILLING_MINUTES.toLong())

    fun estimateTotal(actualDurationMin: Long): Double {
        val billed = billedMinutes(actualDurationMin).toInt()
        return calculateTotal(billed)
    }

    fun calculateTotal(billedMinutes: Int): Double {
        if (billedMinutes <= 0) return 0.0
        var total = MIN_PACKAGE_PRICE_USD
        total += tierCharge(billedMinutes, 31, 60, TIER1_RATE)
        total += tierCharge(billedMinutes, 61, 120, TIER2_RATE)
        total += tierCharge(billedMinutes, 121, 240, TIER3_RATE)
        total += tierCharge(billedMinutes, 241, Int.MAX_VALUE, TIER4_RATE)
        return kotlin.math.round(total * 100.0) / 100.0
    }

    private fun minutesInTier(billedMinutes: Int, tierStart: Int, tierEnd: Int): Int {
        if (billedMinutes < tierStart) return 0
        val effectiveEnd = minOf(billedMinutes, tierEnd)
        return effectiveEnd - tierStart + 1
    }

    private fun tierCharge(billedMinutes: Int, tierStart: Int, tierEnd: Int, rate: Double): Double {
        val minutes = minutesInTier(billedMinutes, tierStart, tierEnd)
        return minutes * rate
    }
}

data class PaymentSummaryResponse(
    val serviceId: Long,
    val actualDurationMin: Int,
    val billedMinutes: Int,
    val totalAmount: Double,
    val ratePerMinute: Double,
    val calculatedAt: String,
    val confirmed: Boolean
)

data class LocationUpdateRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
