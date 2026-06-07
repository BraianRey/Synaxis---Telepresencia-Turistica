package com.synexis.management_service.dto.response;

import com.synexis.management_service.payment.PaymentPricing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Service response DTO with comprehensive information about the service,
 * including client and partner details for summary display.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceResponse {

    private Long serviceId;

    private String clientName;
    private String clientEmail;
    private String clientPicDirectory;

    private String partnerName;
    private String partnerEmail;
    private String partnerPicDirectory;

    private String startLocationDescription;

    private Integer agreedHours;

    private Double hourlyRate;

    private String status;

    private Instant startedAt;

    private Instant endedAt;
    private String locationReferenceImageUrl;
    private Boolean scheduled;
    private Instant scheduledAt;


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
