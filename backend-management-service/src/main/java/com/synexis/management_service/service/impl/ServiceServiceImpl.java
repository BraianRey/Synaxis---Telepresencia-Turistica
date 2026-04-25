package com.synexis.management_service.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synexis.management_service.dto.mapper.ServiceMapper;
import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.PartnerAvailabilityStatus;
import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServiceIdempotencyKey;
import com.synexis.management_service.entity.ServiceStatus;
import com.synexis.management_service.entity.UserStatus;
import com.synexis.management_service.exception.BusinessRuleViolationException;
import com.synexis.management_service.exception.ForbiddenAccessException;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.repository.ServiceIdempotencyKeyRepository;
import com.synexis.management_service.repository.ServiceRepository;
import com.synexis.management_service.service.NotificationService;

import jakarta.persistence.LockModeType;
import com.synexis.management_service.service.PaymentService;
import com.synexis.management_service.service.ServiceHistoryService;
import com.synexis.management_service.service.ServiceService;

@Service
public class ServiceServiceImpl implements ServiceService {

    private static final Set<ServiceStatus> ACTIVE_SERVICE_STATUSES = Set.of(
            ServiceStatus.REQUESTED,
            ServiceStatus.ACCEPTED,
            ServiceStatus.STARTED);

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final PartnerRepository partnerRepository;
    private final ClientRepository clientRepository;
    private final PaymentService paymentService;
    private final ServiceHistoryService serviceHistoryService;
    private final NotificationService notificationService;
    private final ServiceIdempotencyKeyRepository serviceIdempotencyKeyRepository;

    public ServiceServiceImpl(
            ServiceRepository serviceRepository,
            ServiceMapper serviceMapper,
            PartnerRepository partnerRepository,
            ClientRepository clientRepository,
            PaymentService paymentService,
            ServiceHistoryService serviceHistoryService,
            NotificationService notificationService,
            ServiceIdempotencyKeyRepository serviceIdempotencyKeyRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceMapper = serviceMapper;
        this.partnerRepository = partnerRepository;
        this.clientRepository = clientRepository;
        this.paymentService = paymentService;
        this.serviceHistoryService = serviceHistoryService;
        this.notificationService = notificationService;
        this.serviceIdempotencyKeyRepository = serviceIdempotencyKeyRepository;
    }

    @Override
    @Transactional
    public ServiceResponse registerService(RegisterServiceRequest request, Long authenticatedClientId,
            String idempotencyKey) {
        Client client = clientRepository.findById(authenticatedClientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + authenticatedClientId));

        if (client.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Client account is not active");
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = idempotencyKey.trim();
            if (key.length() > 128) {
                throw new BusinessRuleViolationException("Idempotency key must be at most 128 characters");
            }
            Optional<ServiceIdempotencyKey> existing = serviceIdempotencyKeyRepository
                    .findByClientIdAndIdempotencyKey(authenticatedClientId, key);
            if (existing.isPresent()) {
                return getServiceForClient(existing.get().getServiceId(), authenticatedClientId);
            }
        }

        if (serviceRepository.existsByClient_IdAndStatusIn(authenticatedClientId, ACTIVE_SERVICE_STATUSES)) {
            throw new BusinessRuleViolationException(
                    "You already have an active service request. Finish or cancel it before creating another.");
        }

        ServiceEntity service = serviceMapper.toEntity(request, client);
        service.setRequestedAt(LocalDateTime.now());
        service.setStatus(ServiceStatus.REQUESTED);

        ServiceEntity saved = serviceRepository.save(service);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ServiceIdempotencyKey row = new ServiceIdempotencyKey();
            row.setClientId(authenticatedClientId);
            row.setIdempotencyKey(idempotencyKey.trim());
            row.setServiceId(saved.getIdService());
            serviceIdempotencyKeyRepository.save(row);
        }

        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByClientIdForUser(Long clientId, Long authenticatedClientId) {
        if (!clientId.equals(authenticatedClientId)) {
            throw new ForbiddenAccessException("You can only list your own services");
        }
        List<ServiceEntity> services = serviceRepository.findByClient_Id(clientId);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByPartnerIdForUser(Long partnerId, Long authenticatedPartnerId) {
        if (!partnerId.equals(authenticatedPartnerId)) {
            throw new ForbiddenAccessException("You can only list your own assigned services");
        }
        List<ServiceEntity> services = serviceRepository.findByPartner_Id(partnerId);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getAvailableServices() {
        List<ServiceEntity> services = serviceRepository.findByStatus(ServiceStatus.REQUESTED);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceForClient(Long serviceId, Long clientId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
        if (!service.getClient().getId().equals(clientId)) {
            throw new ForbiddenAccessException("You are not allowed to access this service");
        }
        return serviceMapper.toResponse(service);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceForPartner(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found with id: " + partnerId));

        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        if (service.getPartner() != null && service.getPartner().getId().equals(partnerId)) {
            return serviceMapper.toResponse(service);
        }

        if (service.getStatus() == ServiceStatus.REQUESTED) {
            return serviceMapper.toResponse(service);
        }

        throw new ForbiddenAccessException("You are not allowed to access this service");
    }

    @Override
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ServiceResponse acceptService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.REQUESTED) {
            throw new BusinessRuleViolationException("Only REQUESTED services can be accepted");
        }

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found with id: " + partnerId));

        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        if (partner.getAvailabilityStatus() != PartnerAvailabilityStatus.available) {
            throw new BusinessRuleViolationException("Partner is not available to accept services");
        }

        boolean hasActiveService = serviceRepository.existsByPartner_IdAndStatusIn(
                partnerId,
                Set.of(ServiceStatus.ACCEPTED, ServiceStatus.STARTED));

        if (hasActiveService) {
            throw new BusinessRuleViolationException("Partner already has an active service");
        }

        service.setPartner(partner);
        service.setStatus(ServiceStatus.ACCEPTED);
        service.setAcceptedAt(LocalDateTime.now());

        ServiceEntity saved = serviceRepository.save(service);

        serviceHistoryService.recordEvent(
                saved,
                "PARTNER",
                partnerId,
                "Service accepted by partner",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse readyService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Only ACCEPTED services can be set to READY");
        }

        if (!partnerId.equals(service.getPartner().getId())) {
            throw new ForbiddenAccessException("Partner does not own this service");
        }

        service.setStatus(ServiceStatus.READY);
        service.setStartedAt(LocalDateTime.now()); // Assuming started_at is used for ready

        ServiceEntity saved = serviceRepository.save(service);

        serviceHistoryService.recordEvent(
                saved,
                "PARTNER",
                partnerId,
                "Service set to READY by partner",
                Instant.now());

        notificationService.notifyClientServiceReady(saved);

        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse startService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Only ACCEPTED services can be started");
        }

        Partner partner = service.getPartner();
        if (partner == null || !partner.getId().equals(partnerId)) {
            throw new BusinessRuleViolationException("Service is not assigned to this partner");
        }
        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        service.setStatus(ServiceStatus.STARTED);
        service.setStartedAt(LocalDateTime.now());

        ServiceEntity saved = serviceRepository.save(service);

        serviceHistoryService.recordEvent(
                saved,
                "PARTNER",
                partnerId,
                "Service started",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse completeService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.STARTED) {
            throw new BusinessRuleViolationException("Only STARTED services can be completed");
        }

        Partner partner = service.getPartner();
        if (partner == null || !partner.getId().equals(partnerId)) {
            throw new BusinessRuleViolationException("Service is not assigned to this partner");
        }
        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        service.setStatus(ServiceStatus.COMPLETED);
        service.setEndedAt(LocalDateTime.now());

        Partner assignedPartner = service.getPartner();
        if (assignedPartner != null && assignedPartner.getAvailabilityStatus() == PartnerAvailabilityStatus.busy) {
            assignedPartner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
            partnerRepository.save(assignedPartner);
        }

        ServiceEntity saved = serviceRepository.save(service);

        serviceHistoryService.recordEvent(
                saved,
                "PARTNER",
                partnerId,
                "Service completed",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse cancelService(Long serviceId, Long clientId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        if (client.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Client account is not active");
        }

        if (!service.getClient().getId().equals(client.getId())) {
            throw new BusinessRuleViolationException("Client is not the owner of this service");
        }

        if (service.getStatus() == ServiceStatus.COMPLETED || service.getStatus() == ServiceStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Completed or cancelled services cannot be cancelled again");
        }

        if (service.getStatus() == ServiceStatus.STARTED) {
            throw new BusinessRuleViolationException(
                    "In-progress services can only be cancelled by the system due to connection failures");
        }

        if (service.getStatus() != ServiceStatus.REQUESTED && service.getStatus() != ServiceStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Service is not in a cancellable state");
        }

        paymentService.cancelPreAuthorization(serviceId);

        Partner assignedPartner = service.getPartner();
        if (assignedPartner != null && assignedPartner.getAvailabilityStatus() == PartnerAvailabilityStatus.busy) {
            assignedPartner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
            partnerRepository.save(assignedPartner);
        }

        service.setStatus(ServiceStatus.CANCELLED);
        service.setEndedAt(LocalDateTime.now());

        service.setPartner(null);

        ServiceEntity saved = serviceRepository.save(service);

        serviceHistoryService.recordEvent(
                saved,
                "CLIENT",
                clientId,
                "Service cancelled by client",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse cancelServiceByPartner(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        Partner partner = service.getPartner();
        if (partner == null || !partner.getId().equals(partnerId)) {
            throw new BusinessRuleViolationException("Service is not assigned to this partner");
        }
        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        if (service.getStatus() == ServiceStatus.STARTED) {
            throw new BusinessRuleViolationException(
                    "In-progress services cannot be cancelled by the partner");
        }

        if (service.getStatus() != ServiceStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Only ACCEPTED services can be cancelled by the partner");
        }

        service.setStatus(ServiceStatus.CANCELLED);
        service.setEndedAt(LocalDateTime.now());

        ServiceEntity saved = serviceRepository.save(service);

        notificationService.notifyClientServiceCancelledByPartner(saved);

        serviceHistoryService.recordEvent(
                saved,
                "PARTNER",
                partnerId,
                "Service cancelled by partner",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

}
