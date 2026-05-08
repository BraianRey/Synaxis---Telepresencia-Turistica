package com.synexis.management_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSummaryResponse(
        Long serviceId,
        Integer actualDurationMin,
        BigDecimal billedHours,
        BigDecimal totalAmount,
        BigDecimal hourlyRate,
        Instant calculatedAt,
        Boolean confirmed) {
}
