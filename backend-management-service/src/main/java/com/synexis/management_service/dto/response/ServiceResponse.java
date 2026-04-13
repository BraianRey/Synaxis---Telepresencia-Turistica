package com.synexis.management_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(

        Long serviceId,

        Long clientId,
        String clientName,

        Long partnerId,

        Long areaId,

        String startLocationDescription,

        Integer agreedHours,

        BigDecimal hourlyRate,

        String status,

        LocalDateTime requestedAt,

        LocalDateTime acceptedAt,

        LocalDateTime startedAt,

        LocalDateTime endedAt

) {
}
