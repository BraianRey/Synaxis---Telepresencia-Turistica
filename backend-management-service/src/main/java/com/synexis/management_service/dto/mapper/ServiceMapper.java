package com.synexis.management_service.dto.mapper;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.entity.Area;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.AreaRepository;
import com.synexis.management_service.repository.ClientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AreaRepository areaRepository;

    public ServiceEntity toEntity(RegisterServiceRequest request) {

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));

        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("Area not found with id: " + request.areaId()));

        ServiceEntity service = new ServiceEntity();

        service.setClient(client);
        service.setArea(area);
        service.setStartLocationDescription(request.startLocationDescription());
        service.setAgreedHours(request.agreedHours());
        service.setHourlyRate(request.hourlyRate());

        return service;
    }

    public ServiceResponse toResponse(ServiceEntity service) {

        return new ServiceResponse(
                service.getIdService(),
                service.getClient().getId(),
                service.getClient().getName(),
                service.getPartner() != null ? service.getPartner().getId() : null,
                service.getArea().getId(),
                service.getStartLocationDescription(),
                service.getAgreedHours(),
                service.getHourlyRate(),
                service.getStatus().name(),
                service.getRequestedAt(),
                service.getAcceptedAt(),
                service.getStartedAt(),
                service.getEndedAt());
    }
}
