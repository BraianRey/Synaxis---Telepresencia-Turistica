package com.sismptm.client.data.remote.api.dto

import java.util.Locale

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
    val partnerPicDirectory: String? = null,
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
        } catch (e: java.time.format.DateTimeParseException) {
            android.util.Log.e("ServiceDtos", "Failed to parse date", e)
            null
        } catch (e: ArithmeticException) {
            android.util.Log.e("ServiceDtos", "Duration overflow", e)
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
        return String.format(Locale.US, "$%.2f USD", cost)
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
