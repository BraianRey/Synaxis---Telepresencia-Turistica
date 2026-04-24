package com.synexis.management_service.dto.request;

import jakarta.validation.constraints.*;

public record RegisterServiceRequest(

        @NotNull Long clientId,

        @NotNull Double longitude,

        @NotNull Double latitude,

        @Size(max = 255) String startLocationDescription,

        @NotNull @Min(1) Integer agreedHours

) {
}