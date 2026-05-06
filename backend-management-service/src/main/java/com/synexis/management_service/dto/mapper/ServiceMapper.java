package com.synexis.management_service.dto.mapper;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.ServiceEntity;

import org.springframework.stereotype.Component;

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
                service.getClient().getId(),
                service.getClient().getName(),
                service.getClient().getEmail(),
                service.getClient().getPicDirectory(),
                // Partner info
                partner != null ? partner.getId() : null,
                partner != null ? partner.getName() : null,
                partner != null ? partner.getEmail() : null,
                partner != null ? partner.getPicDirectory() : null,
                // Service details
                service.getLongitude(),
                service.getLatitude(),
                service.getStartLocationDescription(),
                service.getAgreedHours(),
                DEFAULT_HOURLY_RATE,
                service.getStatus().name(),
                service.getRequestedAt(),
                service.getAcceptedAt(),
                service.getStartedAt(),
                service.getEndedAt());
    }
}
