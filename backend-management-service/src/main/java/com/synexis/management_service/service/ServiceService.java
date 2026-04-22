package com.synexis.management_service.service;

import java.util.List;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;

public interface ServiceService {

    /**
     * Creates a service for {@code authenticatedClientId} (from JWT). Optional
     * {@code idempotencyKey} ties repeated creates to the same service.
     */
    ServiceResponse registerService(RegisterServiceRequest request, Long authenticatedClientId, String idempotencyKey);

    List<ServiceResponse> getServicesByClientIdForUser(Long clientId, Long authenticatedClientId);

    List<ServiceResponse> getServicesByPartnerIdForUser(Long partnerId, Long authenticatedPartnerId);

    List<ServiceResponse> getAvailableServices();

    ServiceResponse getServiceForClient(Long serviceId, Long clientId);

    ServiceResponse getServiceForPartner(Long serviceId, Long partnerId);

    ServiceResponse acceptService(Long serviceId, Long partnerId);

    ServiceResponse startService(Long serviceId, Long partnerId);

    ServiceResponse completeService(Long serviceId, Long partnerId);

    ServiceResponse cancelService(Long serviceId, Long clientId);

    ServiceResponse cancelServiceByPartner(Long serviceId, Long partnerId);

}
