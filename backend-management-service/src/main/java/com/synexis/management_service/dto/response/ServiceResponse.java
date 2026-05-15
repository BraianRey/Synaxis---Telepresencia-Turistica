package com.synexis.management_service.dto.response;

import java.time.Instant;

/**
 * Service response DTO with comprehensive information about the service,
 * including client and partner details for summary display.
 */
public record ServiceResponse(

        Long serviceId,

        String clientName,
        String clientEmail,

        String partnerName,
        String partnerEmail,

        String startLocationDescription,

        Integer agreedHours,

        Double hourlyRate,

        String status,

        Instant startedAt,

        Instant endedAt

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
     * Calculates the total cost based on duration and hourly rate.
     * Returns null if duration cannot be calculated.
     */
    public Double getTotalCost() {
        Long durationMinutes = getDurationMinutes();
        if (durationMinutes == null || hourlyRate == null) {
            return null;
        }
        if (durationMinutes <= 60) {
            return hourlyRate;
        }
        long excessMinutes = durationMinutes - 60;
        double excessCost = (excessMinutes / 60.0) * hourlyRate;
        return Math.round((hourlyRate + excessCost) * 100.0) / 100.0;
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
        return String.format("$%,.0f COP", cost);
    }
}
