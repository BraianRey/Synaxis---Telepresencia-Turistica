package com.synexis.management_service.dto.response;

import com.synexis.management_service.payment.PaymentPricing;

import java.time.Instant;

/**
 * Service response DTO with comprehensive information about the service,
 * including client and partner details for summary display.
 */
public record ServiceResponse(

        Long serviceId,

        String clientName,
        String clientEmail,
        String clientPicDirectory,

        String partnerName,
        String partnerEmail,

        String startLocationDescription,

        Integer agreedHours,

        Double hourlyRate,

        String status,

        Instant startedAt,

        Instant endedAt,
        String locationReferenceImageUrl

) {
    /**
     * Calculates the service duration in minutes.
     * Returns null if the service hasn't started or ended.
     */
    public Long getDurationMinutes() {
        if (startedAt == null || endedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, endedAt).toMinutes();
    }

    /**
     * Estimates total cost: minimum {@link PaymentPricing#MIN_BILLING_MINUTES} minutes
     * at {@link PaymentPricing#MIN_PACKAGE_PRICE_USD} USD, then per minute.
     */
    public Double getTotalCost() {
        Long durationMinutes = getDurationMinutes();
        if (durationMinutes == null) {
            return null;
        }
        return PaymentPricing.estimateTotalFromActualMinutes(durationMinutes.intValue()).doubleValue();
    }

    /**
     * Gets formatted duration string (e.g., "45 min" or "1h 30m").
     */
    public String getFormattedDuration() {
        Long minutes = getDurationMinutes();
        if (minutes == null) {
            return "N/A";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return hours + "h";
        }
        return hours + "h " + remainingMinutes + "m";
    }

    /**
     * Gets formatted cost string with currency.
     */
    public String getFormattedCost() {
        Double cost = getTotalCost();
        if (cost == null) {
            return "N/A";
        }
        return String.format("$%.2f USD", cost);
    }
}
