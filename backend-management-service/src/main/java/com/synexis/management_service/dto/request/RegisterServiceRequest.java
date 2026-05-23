package com.synexis.management_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
     * Defaults to false when omitted.
     * When true, scheduledFor must be provided and must be in the future.
     */
    private Boolean scheduled;

    /**
     * Required only when scheduled = true.
     * Must be a future date/time — validated in the service layer
     * because the constraint is conditional on 'scheduled'.
     */
    private LocalDateTime scheduledFor;
}