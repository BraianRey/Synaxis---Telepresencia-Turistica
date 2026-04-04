package com.synexis.management_service.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RegisterServiceRequest(

        @NotNull Long areaId,

        @Size(max = 255) String startLocationDescription,

        @NotNull @Min(1) Integer agreedHours,

        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal hourlyRate

) {
}