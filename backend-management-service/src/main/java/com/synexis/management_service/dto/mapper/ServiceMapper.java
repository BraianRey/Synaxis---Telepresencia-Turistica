package com.synexis.management_service.dto.mapper;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.payment.PaymentPricing;

import org.springframework.stereotype.Component;
import java.time.ZoneOffset;

@Component
public class ServiceMapper {

        public ServiceEntity toEntity(RegisterServiceRequest request, Client client) {

                ServiceEntity service = new ServiceEntity();

                service.setClient(client);
                service.setLongitude(request.getLongitude());
                service.setLatitude(request.getLatitude());
                service.setStartLocationDescription(request.getStartLocationDescription());
                service.setAgreedHours(request.getAgreedHours());

                return service;
        }

        public ServiceResponse toResponse(ServiceEntity service) {
                Partner partner = service.getPartner();
                String clientPic = service.getClient().getPicDirectory();
                System.out.println("[ServiceMapper] Service " + service.getIdService() + ": client=" + service.getClient().getName() + ", picDirectory=" + clientPic);

                return new ServiceResponse(
                                service.getIdService(),
                                // Client info
                                service.getClient().getName(),
                                service.getClient().getEmail(),
                                clientPic,
                                // Partner info
                                partner != null ? partner.getName() : null,
                                partner != null ? partner.getEmail() : null,
                                partner != null ? partner.getPicDirectory() : null,
                                // Service details
                                service.getStartLocationDescription(),
                                service.getAgreedHours(),
                                Double.valueOf(PaymentPricing.EQUIVALENT_HOURLY_RATE_USD),
                                service.getStatus().name(),
                                service.getStartedAt() != null
                                                ? service.getStartedAt().atZone(ZoneOffset.UTC)
                                                                .toInstant()
                                                : null,
                                service.getEndedAt() != null
                                                ? service.getEndedAt().atZone(ZoneOffset.UTC)
                                                                .toInstant()
                                                : null,
                                service.getLocationReferenceImageUrl(),
                                service.isScheduled(),
                                service.getScheduledFor() != null
                                                ? service.getScheduledFor().atZone(ZoneOffset.UTC)
                                                                .toInstant()
                                                : null);
        }
}
