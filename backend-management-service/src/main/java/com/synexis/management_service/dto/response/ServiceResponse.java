    public ServiceResponse(Long serviceId, Long clientId, Long partnerId, Long areaId, String startLocationDescription,
package com.synexis.management_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(

        Long serviceId,

        Long clientId,
        String clientName,



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
    public ServiceResponse(Long serviceId, Long clientId, String clientName, Long partnerId, Long areaId,
            String startLocationDescription,
            Integer agreedHours, BigDecimal hourlyRate, String status, LocalDateTime requestedAt,
            LocalDateTime acceptedAt, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.serviceId = serviceId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.partnerId = partnerId;
        this.areaId = areaId;
        this.startLocationDescription = startLocationDescription;
        this.agreedHours = agreedHours;
        this.hourlyRate = hourlyRate;
        this.status = status;
        this.requestedAt = requestedAt;
        this.acceptedAt = acceptedAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }
}
