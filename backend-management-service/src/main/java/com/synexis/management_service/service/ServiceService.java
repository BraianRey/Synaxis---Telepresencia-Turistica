package com.synexis.management_service.service;

import java.util.List;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;

public interface ServiceService {

    ServiceResponse registerService(RegisterServiceRequest request);

    List<ServiceResponse> getServicesByClientId(Long clientId);

    List<ServiceResponse> getServicesByPartnerId(Long partnerId);

    List<ServiceResponse> getServicesAvailableByAreaId(Long areaId);

    ServiceResponse getServicesByServiceId(Long serviceId);

    ServiceResponse acceptService(Long serviceId, Long partnerId);

    ServiceResponse startService(Long serviceId, Long partnerId);

    ServiceResponse completeService(Long serviceId, Long partnerId);

    ServiceResponse cancelService(Long serviceId, Long clientId);

}
