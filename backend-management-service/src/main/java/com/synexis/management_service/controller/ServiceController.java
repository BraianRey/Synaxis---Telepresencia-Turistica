package com.synexis.management_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.service.ServiceService;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceResponse createService(@RequestBody RegisterServiceRequest request) {
        return serviceService.registerService(request);
    }

    @GetMapping("/client/{clientId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('CLIENT')")
    public List<ServiceResponse> getServicesByClientId(@PathVariable Long clientId) {
        return serviceService.getServicesByClientId(clientId);
    }

    @GetMapping("/partner/{partnerId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public List<ServiceResponse> getServicesByPartnerId(@PathVariable Long partnerId) {
        return serviceService.getServicesByPartnerId(partnerId);
    }

    @GetMapping("/available/{areaId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public List<ServiceResponse> getServicesAvailableByAreaId(@PathVariable Long areaId) {
        return serviceService.getServicesAvailableByAreaId(areaId);
    }

    @GetMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENT', 'PARTNER')")
    public ServiceResponse getServicesByServiceId(@PathVariable Long serviceId) {
        return serviceService.getServicesByServiceId(serviceId);
    }

}
