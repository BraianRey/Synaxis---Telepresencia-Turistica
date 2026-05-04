package com.synexis.management_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSummaryResponse(
        Long serviceId,
        Integer actualDurationMin,
        BigDecimal billedHours,
        BigDecimal totalAmount,
        BigDecimal hourlyRate,
        LocalDateTime calculatedAt,
        Boolean confirmed) {
}
