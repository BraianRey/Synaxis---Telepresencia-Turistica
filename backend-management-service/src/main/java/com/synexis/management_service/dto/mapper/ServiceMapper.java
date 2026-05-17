package com.synexis.management_service.dto.mapper;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.ServiceEntity;

import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class ServiceMapper {

        // Default hourly rate in COP
        private static final double DEFAULT_HOURLY_RATE = 15000.0;

        public ServiceEntity toEntity(RegisterServiceRequest request, Client client) {

                ServiceEntity service = new ServiceEntity();

                service.setClient(client);
                service.setLongitude(request.longitude());
                service.setLatitude(request.latitude());
                service.setStartLocationDescription(request.startLocationDescription());
                service.setAgreedHours(request.agreedHours());

                return service;
        }

        public ServiceResponse toResponse(ServiceEntity service) {
                Partner partner = service.getPartner();

                return new ServiceResponse(
                                service.getIdService(),
                                // Client info
                                service.getClient().getName(),
                                service.getClient().getEmail(),
                                // Partner info
                                partner != null ? partner.getName() : null,
                                partner != null ? partner.getEmail() : null,
                                // Service details
                                service.getStartLocationDescription(),
                                service.getPayment() != null
                                                ? Integer.valueOf(
                                                                service.getPayment().getBilledHours()
                                                                                .setScale(0, RoundingMode.HALF_UP)
                                                                                .intValue())
                                                : service.getAgreedHours(),
                                Double.valueOf(DEFAULT_HOURLY_RATE),
                                service.getStatus().name(),
                                service.getStartedAt() != null
                                                ? service.getStartedAt().atZone(java.time.ZoneId.systemDefault())
                                                                .toInstant()
                                                : null,
                                service.getEndedAt() != null
                                                ? service.getEndedAt().atZone(java.time.ZoneId.systemDefault())
                                                                .toInstant()
                                                : null,
                                service.getLocationReferenceImageUrl());
        }
}
