package com.synexis.management_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.synexis.management_service.dto.mapper.ServiceMapper;
import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServiceStatus;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.ServiceRepository;
import com.synexis.management_service.service.ServiceService;

@Service
public class ServiceServiceImpl implements ServiceService {

    private ServiceRepository serviceRepository;
    private ServiceMapper serviceMapper;

    public ServiceServiceImpl(ServiceRepository serviceRepository, ServiceMapper serviceMapper) {
        this.serviceRepository = serviceRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public ServiceResponse registerService(RegisterServiceRequest request) {
        ServiceEntity service = serviceMapper.toEntity(request);

        service.setRequestedAt(LocalDateTime.now());
        service.setStatus(ServiceStatus.REQUESTED);

        ServiceEntity savedService = serviceRepository.save(service);
        return serviceMapper.toResponse(savedService);
    }

    @Override
    public List<ServiceResponse> getServicesByClientId(Long clientId) {
        // Implementation for fetching services by client ID
        List<ServiceEntity> services = serviceRepository.findByClient_Id(clientId);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ServiceResponse> getServicesByPartnerId(Long partnerId) {
        // Implementation for fetching services by partner ID
        List<ServiceEntity> services = serviceRepository.findByPartner_Id(partnerId);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ServiceResponse> getServicesAvailableByAreaId(Long areaId) {
        throw new UnsupportedOperationException("Unimplemented method 'getServicesAvailableByAreaId'");
    }

    @Override
    public ServiceResponse getServicesByServiceId(Long serviceId) {
        // Implementation for fetching service by service ID
        ServiceEntity service = serviceRepository.findById(serviceId).orElse(null);

        if (service == null) {
            throw new ResourceNotFoundException("Service not found with id: " + serviceId);
        }

        return serviceMapper.toResponse(service);
    }

    @Override
    public ServiceResponse acceptService(Long serviceId, Long partnerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'acceptService'");
    }

    @Override
    public ServiceResponse startService(Long serviceId, Long partnerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startService'");
    }

    @Override
    public ServiceResponse completeService(Long serviceId, Long partnerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'completeService'");
    }

    @Override
    public ServiceResponse cancelService(Long serviceId, Long clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cancelService'");
    }

}
