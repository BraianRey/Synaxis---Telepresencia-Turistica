package com.synexis.management_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class RegisterServiceRequest {

    @NotNull
    private Double longitude;

    @NotNull
    private Double latitude;

    @Size(max = 255)
    private String startLocationDescription;

    @Min(1)
    private Integer agreedHours;

    /**
     * ISO 8601 formatted timestamp with timezone offset (e.g., "2026-05-28T15:00:00+09:00")
     * When provided, the service is treated as scheduled.
     * When null or empty, the service is treated as immediate.
     */
    private String scheduledAt;
}