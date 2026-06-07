package com.synexis.management_service.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synexis.management_service.dto.mapper.ServiceMapper;
import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.PaymentSummaryResponse;
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
import com.synexis.management_service.repository.ServicePaymentRepository;
import com.synexis.management_service.repository.ServiceRepository;
import com.synexis.management_service.service.NotificationService;
import com.synexis.management_service.client.WikimediaClient;

import jakarta.persistence.LockModeType;
import com.synexis.management_service.service.PaymentService;
import com.synexis.management_service.service.ServiceHistoryService;
import com.synexis.management_service.service.ServiceService;

/**
 * Service implementation for managing tours, assignments, and statuses.
 */
@Service
public class ServiceServiceImpl implements ServiceService {

    private static final Set<ServiceStatus> ACTIVE_SERVICE_STATUSES = Set.of(
            ServiceStatus.REQUESTED,
            ServiceStatus.ACCEPTED,
            ServiceStatus.WAITING_FOR_START,
            ServiceStatus.READY,
            ServiceStatus.IN_PROGRESS);

    private static final Set<ServiceStatus> PARTNER_ACTIVE_STATUSES = Set.of(
            ServiceStatus.ACCEPTED,
            ServiceStatus.WAITING_FOR_START,
            ServiceStatus.READY,
            ServiceStatus.IN_PROGRESS);

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final PartnerRepository partnerRepository;
    private final ClientRepository clientRepository;
    private final PaymentService paymentService;
    private final ServicePaymentRepository servicePaymentRepository;
    private final ServiceHistoryService serviceHistoryService;
    private final NotificationService notificationService;
    private final WikimediaClient wikimediaClient;
    private final ServiceIdempotencyKeyRepository serviceIdempotencyKeyRepository;

    public ServiceServiceImpl(
            ServiceRepository serviceRepository,
            ServiceMapper serviceMapper,
            PartnerRepository partnerRepository,
            ClientRepository clientRepository,
            PaymentService paymentService,
            ServicePaymentRepository servicePaymentRepository,
            ServiceHistoryService serviceHistoryService,
            NotificationService notificationService,
            WikimediaClient wikimediaClient,
            ServiceIdempotencyKeyRepository serviceIdempotencyKeyRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceMapper = serviceMapper;
        this.partnerRepository = partnerRepository;
        this.clientRepository = clientRepository;
        this.paymentService = paymentService;
        this.servicePaymentRepository = servicePaymentRepository;
        this.serviceHistoryService = serviceHistoryService;
        this.notificationService = notificationService;
        this.wikimediaClient = wikimediaClient;
        this.serviceIdempotencyKeyRepository = serviceIdempotencyKeyRepository;
    }

    /**
     * Registers a new tour service, including scheduled reservations.
     * @param request Tour data.
     * @param authenticatedClientId Requesting client ID.
     * @param idempotencyKey Key to prevent duplicates.
     * @return Created service details.
     */
    @Override
    @Transactional
    public ServiceResponse registerService(RegisterServiceRequest request, Long authenticatedClientId,
            String idempotencyKey) {

        Client client = clientRepository.findById(authenticatedClientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with id: " + authenticatedClientId));

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

        boolean isScheduled = request.getScheduledAt() != null && !request.getScheduledAt().isBlank();
        java.time.OffsetDateTime scheduledFor = null;

        if (isScheduled) {
            try {
                scheduledFor = java.time.OffsetDateTime.parse(request.getScheduledAt());
                if (!scheduledFor.isAfter(java.time.OffsetDateTime.now())) {
                    throw new BusinessRuleViolationException(
                            "scheduledAt must be a future date/time");
                }
            } catch (java.time.format.DateTimeParseException e) {
                throw new BusinessRuleViolationException(
                        "Invalid scheduledAt format. Expected ISO 8601 with timezone");
            }
        }

        if (serviceRepository.existsByClient_IdAndStatusIn(authenticatedClientId, ACTIVE_SERVICE_STATUSES)) {
            throw new BusinessRuleViolationException(
                    "You already have an active service request. Finish or cancel it before creating another.");
        }

        ServiceEntity service = serviceMapper.toEntity(request, client);
        service.setRequestedAt(LocalDateTime.now());
        service.setStatus(ServiceStatus.REQUESTED);
        service.setScheduled(isScheduled);

        if (isScheduled) {
            java.time.Instant utcInstant = scheduledFor.toInstant();
            LocalDateTime utcLocalDateTime = utcInstant.atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
            service.setScheduledFor(utcLocalDateTime);
            
            if (request.getAgreedHours() != null) {
                service.setScheduledEndAt(utcLocalDateTime.plusHours(request.getAgreedHours()));
            }
        }

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

    /**
     * Retrieves all services associated with a client.
     * @param clientId Client ID.
     * @param authenticatedClientId Authenticated user ID for security.
     * @return List of client services.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByClientIdForUser(Long clientId, Long authenticatedClientId) {
        if (!clientId.equals(authenticatedClientId)) {
            throw new ForbiddenAccessException("You can only list your own services");
        }
        List<ServiceEntity> services = serviceRepository.findByClient_Id(clientId);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Retrieves all services assigned to a partner.
     * @param partnerId Partner ID.
     * @param authenticatedPartnerId Authenticated partner ID for security.
     * @return List of partner services.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByPartnerIdForUser(Long partnerId, Long authenticatedPartnerId) {
        if (!partnerId.equals(authenticatedPartnerId)) {
            throw new ForbiddenAccessException("You can only list your own assigned services");
        }
        List<ServiceEntity> services = serviceRepository.findByPartner_Id(partnerId);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Lists services available to be accepted by partners.
     * @return List of services with REQUESTED status.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getAvailableServices() {
        List<ServiceEntity> services = serviceRepository.findByStatus(ServiceStatus.REQUESTED);
        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Retrieves a specific service, validating it belongs to the client.
     * @param serviceId Service ID.
     * @param clientId Requesting client ID.
     * @return Service details response.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getActiveServicesByClient(Long clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with id: " + clientId
                ));

        List<ServiceEntity> services = serviceRepository.findByClient_IdAndStatusIn(
                clientId,
                List.of(
                        ServiceStatus.ACCEPTED,
                        ServiceStatus.WAITING_FOR_START,
                        ServiceStatus.READY,
                        ServiceStatus.IN_PROGRESS
                )
        );

        return services.stream().map(serviceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceForClient(Long serviceId, Long clientId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));
        if (!service.getClient().getId().equals(clientId)) {
            throw new ForbiddenAccessException("You are not allowed to access this service");
        }
        return serviceMapper.toResponse(service);
    }

    /**
     * Retrieves a specific service, validating it belongs to the partner.
     * @param serviceId Service ID.
     * @param partnerId Requesting partner ID.
     * @return Service details response.
     */
    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceForPartner(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Partner not found with id: " + partnerId));

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

    /**
     * Generates a financial summary for a completed service.
     * @param serviceId Service ID.
     * @return Payment and duration summary.
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(Long serviceId) {
        com.synexis.management_service.entity.ServicePayment payment = servicePaymentRepository
                .findByService_IdService(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for service: " + serviceId));

        return new PaymentSummaryResponse(
                payment.getService().getIdService(),
                payment.getActualDurationMin(),
                payment.getBilledMinutes(),
                payment.getTotalAmount(),
                payment.getRatePerMinute(),
                payment.getCalculatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                payment.getConfirmed());
    }

    /**
     * Confirms the payment for a service.
     * @param serviceId Service ID.
     * @return Confirmed payment summary.
     */
    @Override
    @Transactional
    public PaymentSummaryResponse confirmPayment(Long serviceId) {
        com.synexis.management_service.entity.ServicePayment payment = servicePaymentRepository
                .findByService_IdService(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for service: " + serviceId));

        if (!Boolean.TRUE.equals(payment.getConfirmed())) {
            payment.setConfirmed(true);
            payment = servicePaymentRepository.save(payment);
        }

        return new PaymentSummaryResponse(
                payment.getService().getIdService(),
                payment.getActualDurationMin(),
                payment.getBilledMinutes(),
                payment.getTotalAmount(),
                payment.getRatePerMinute(),
                payment.getCalculatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                payment.getConfirmed());
    }

    /**
     * Allows a partner to accept a requested service.
     * @param serviceId Service ID to accept.
     * @param partnerId Partner ID.
     * @return Updated service with assigned partner.
     */
    @Override
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ServiceResponse acceptService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.REQUESTED) {
            throw new BusinessRuleViolationException("Only REQUESTED services can be accepted");
        }

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Partner not found with id: " + partnerId));

        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        if (partner.getAvailabilityStatus() != PartnerAvailabilityStatus.available) {
            throw new BusinessRuleViolationException("Partner is not available to accept services");
        }

        boolean hasActiveService = serviceRepository.existsByPartner_IdAndStatusIn(
                partnerId, PARTNER_ACTIVE_STATUSES);

        if (hasActiveService) {
            throw new BusinessRuleViolationException("Partner already has an active service");
        }

        service.setPartner(partner);
        service.setAcceptedAt(LocalDateTime.now());

        if (service.isScheduled()) {
            service.setStatus(ServiceStatus.WAITING_FOR_START);
        } else {
            service.setStatus(ServiceStatus.ACCEPTED);
        }

        ServiceEntity saved = serviceRepository.save(service);

        serviceHistoryService.recordEvent(
                saved,
                "PARTNER",
                partnerId,
                "Service accepted by partner",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

    /**
     * Sets the service to READY status.
     * @param serviceId Service ID.
     * @param partnerId Assigned partner ID.
     * @return Updated service with READY status.
     */
    @Override
    @Transactional
    public ServiceResponse readyService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.ACCEPTED
                && service.getStatus() != ServiceStatus.WAITING_FOR_START) {
            throw new BusinessRuleViolationException(
                    "Only ACCEPTED or WAITING_FOR_START services can be set to READY");
        }

        // IMPROVEMENT: Added 1-minute grace period to account for clock skew
        if (service.isScheduled()
                && service.getScheduledFor() != null
                && LocalDateTime.now(ZoneOffset.UTC).isBefore(service.getScheduledFor().minusMinutes(1))) {
            throw new BusinessRuleViolationException(
                    "Scheduled service cannot be set to READY before its scheduled time (1-min grace allowed): "
                            + service.getScheduledFor() + " UTC");
        }

        if (!partnerId.equals(service.getPartner().getId())) {
            throw new ForbiddenAccessException("Partner does not own this service");
        }

        service.setStatus(ServiceStatus.READY);
        service.setStartedAt(LocalDateTime.now());

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

    /**
     * Formally starts the tour (IN_PROGRESS status).
     * @param serviceId Service ID.
     * @param partnerId Assigned partner ID.
     * @return Updated service with IN_PROGRESS status.
     */
    @Override
    @Transactional
    public ServiceResponse startService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.READY) {
            throw new BusinessRuleViolationException("Only READY services can be started");
        }

        Partner partner = service.getPartner();
        if (partner == null || !partner.getId().equals(partnerId)) {
            throw new BusinessRuleViolationException("Service is not assigned to this partner");
        }
        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        // IMPROVEMENT: Added 1-minute grace period to account for clock skew
        if (service.isScheduled()
                && service.getScheduledFor() != null
                && LocalDateTime.now(ZoneOffset.UTC).isBefore(service.getScheduledFor().minusMinutes(1))) {
            throw new BusinessRuleViolationException(
                    "Scheduled service cannot be started before its scheduled time (1-min grace allowed): "
                            + service.getScheduledFor() + " UTC");
        }

        service.setStatus(ServiceStatus.IN_PROGRESS);
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

    /**
     * Completes a service from the partner side.
     * @param serviceId Service ID.
     * @param partnerId Assigned partner ID.
     * @return Completed service details.
     */
    @Override
    @Transactional
    public ServiceResponse completeService(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException("Only IN_PROGRESS services can be completed");
        }

        Partner partner = service.getPartner();
        if (partner == null || !partner.getId().equals(partnerId)) {
            throw new BusinessRuleViolationException("Service is not assigned to this partner");
        }
        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        service.setStatus(ServiceStatus.COMPLETED);
        String imageUrl = wikimediaClient.getLocationImageUrl(service.getLatitude(), service.getLongitude());
        service.setLocationReferenceImageUrl(imageUrl);
        service.setEndedAt(LocalDateTime.now());

        Partner assignedPartner = service.getPartner();
        if (assignedPartner != null
                && assignedPartner.getAvailabilityStatus() == PartnerAvailabilityStatus.busy) {
            assignedPartner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
            partnerRepository.save(assignedPartner);
        }

        ServiceEntity saved = serviceRepository.save(service);
        ((NoopPaymentService) paymentService).calculateAndPersist(saved);

        serviceHistoryService.recordEvent(
                saved,
                "PARTNER",
                partnerId,
                "Service completed",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

    /**
     * Completes a service from the client side.
     * @param serviceId Service ID.
     * @param clientId Owner client ID.
     * @return Completed service details.
     */
    @Override
    @Transactional
    public ServiceResponse completeServiceByClient(Long serviceId, Long clientId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        if (service.getStatus() != ServiceStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException("Only IN_PROGRESS services can be completed");
        }

        Client client = service.getClient();
        if (client == null || !client.getId().equals(clientId)) {
            throw new ForbiddenAccessException("Client is not the owner of this service");
        }

        service.setStatus(ServiceStatus.COMPLETED);
        String imageUrl = wikimediaClient.getLocationImageUrl(service.getLatitude(), service.getLongitude());
        service.setLocationReferenceImageUrl(imageUrl);
        service.setEndedAt(LocalDateTime.now());

        Partner assignedPartner = service.getPartner();
        if (assignedPartner != null
                && assignedPartner.getAvailabilityStatus() == PartnerAvailabilityStatus.busy) {
            assignedPartner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
            partnerRepository.save(assignedPartner);
        }

        ServiceEntity saved = serviceRepository.save(service);
        ((NoopPaymentService) paymentService).calculateAndPersist(saved);

        serviceHistoryService.recordEvent(
                saved,
                "CLIENT",
                clientId,
                "Service completed by client",
                Instant.now());

        return serviceMapper.toResponse(saved);
    }

    /**
     * Cancels a requested or accepted service from the client side.
     * @param serviceId Service ID to cancel.
     * @param clientId Owner client ID.
     * @return Cancelled service details.
     */
    @Override
    @Transactional
    public ServiceResponse cancelService(Long serviceId, Long clientId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with id: " + clientId));

        if (client.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Client account is not active");
        }

        if (!service.getClient().getId().equals(client.getId())) {
            throw new BusinessRuleViolationException("Client is not the owner of this service");
        }

        if (service.getStatus() == ServiceStatus.COMPLETED
                || service.getStatus() == ServiceStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Completed or cancelled services cannot be cancelled again");
        }

        if (service.getStatus() == ServiceStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException(
                    "In-progress services can only be cancelled by the system due to connection failures");
        }

        if (!Set.of(
                ServiceStatus.REQUESTED,
                ServiceStatus.ACCEPTED,
                ServiceStatus.WAITING_FOR_START,
                ServiceStatus.READY).contains(service.getStatus())) {
            throw new BusinessRuleViolationException("Service is not in a cancellable state");
        }

        paymentService.cancelPreAuthorization(serviceId);

        Partner assignedPartner = service.getPartner();
        if (assignedPartner != null
                && assignedPartner.getAvailabilityStatus() == PartnerAvailabilityStatus.busy) {
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

    /**
     * Cancels an accepted service from the assigned partner side.
     * @param serviceId Service ID to cancel.
     * @param partnerId Assigned partner ID.
     * @return Cancelled service details.
     */
    @Override
    @Transactional
    public ServiceResponse cancelServiceByPartner(Long serviceId, Long partnerId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        Partner partner = service.getPartner();
        if (partner == null || !partner.getId().equals(partnerId)) {
            throw new BusinessRuleViolationException("Service is not assigned to this partner");
        }
        if (partner.getStatus() != UserStatus.active) {
            throw new BusinessRuleViolationException("Partner account is not active");
        }

        if (service.getStatus() == ServiceStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException(
                    "In-progress services cannot be cancelled by the partner");
        }

        if (!Set.of(
                ServiceStatus.ACCEPTED,
                ServiceStatus.WAITING_FOR_START,
                ServiceStatus.READY).contains(service.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Only ACCEPTED, WAITING_FOR_START, or READY services can be cancelled by the partner");
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
