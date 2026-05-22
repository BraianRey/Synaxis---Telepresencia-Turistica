package com.synexis.management_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSummaryResponse(
        Long serviceId,
        Integer actualDurationMin,
        Integer billedMinutes,
        BigDecimal totalAmount,
        BigDecimal ratePerMinute,
        Instant calculatedAt,
        Boolean confirmed) {
}
