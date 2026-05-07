package com.synexis.management_service.dto.response;

import java.time.LocalDateTime;

/**
 * Service response DTO with comprehensive information about the service,
 * including client and partner details for summary display.
 */
public record ServiceResponse(

        Long serviceId,

        Long clientId,
        String clientName,
        String clientEmail,
        String clientPicDirectory,

        Long partnerId,
        String partnerName,
        String partnerEmail,
        String partnerPicDirectory,

        Double longitude,

        Double latitude,

        String startLocationDescription,

        Integer agreedHours,

        Double hourlyRate,

        String status,

        LocalDateTime requestedAt,

        LocalDateTime acceptedAt,

        LocalDateTime startedAt,

        LocalDateTime endedAt

) {
    public ServiceResponse(Long serviceId, Long clientId, String clientName, String clientEmail, String clientPicDirectory,
            Long partnerId, String partnerName, String partnerEmail, String partnerPicDirectory,
            Double longitude, Double latitude, String startLocationDescription, Integer agreedHours,
            Double hourlyRate, String status, LocalDateTime requestedAt, LocalDateTime acceptedAt,
            LocalDateTime startedAt, LocalDateTime endedAt) {
        this.serviceId = serviceId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.clientPicDirectory = clientPicDirectory;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.partnerEmail = partnerEmail;
        this.partnerPicDirectory = partnerPicDirectory;
        this.longitude = longitude;
        this.latitude = latitude;
        this.startLocationDescription = startLocationDescription;
        this.agreedHours = agreedHours;
        this.hourlyRate = hourlyRate;
        this.status = status;
        this.requestedAt = requestedAt;
        this.acceptedAt = acceptedAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

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
        // Convert minutes to hours and multiply by rate
        double hours = durationMinutes / 60.0;
        return Math.round(hours * hourlyRate * 100.0) / 100.0; // Round to 2 decimal places
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
