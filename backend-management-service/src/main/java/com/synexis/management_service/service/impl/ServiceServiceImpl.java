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

@Service
public class ServiceServiceImpl implements ServiceService {

    // -------------------------------------------------------------------------
    // ACTIVE_SERVICE_STATUSES
    // WAITING_FOR_START and READY added: a client must not open a second service
    // while a scheduled (or transitioning) one is still live.
    // -------------------------------------------------------------------------
    private static final Set<ServiceStatus> ACTIVE_SERVICE_STATUSES = Set.of(
            ServiceStatus.REQUESTED,
            ServiceStatus.ACCEPTED,
            ServiceStatus.WAITING_FOR_START,
            ServiceStatus.READY,
            ServiceStatus.IN_PROGRESS);

    // Partner-side active statuses reused in acceptService to prevent double-booking.
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

    // -------------------------------------------------------------------------
    // registerService()
    // register service with or without scheduling. Scheduled services start in WAITING_FOR_START
    // status and only transition to READY when the partner confirms they're ready at the scheduled time.
    // -------------------------------------------------------------------------
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

        // Idempotency check: if a key is provided, look for an existing service linked to this client and key.
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

        // Parse scheduledAt to determine if service is scheduled
        boolean isScheduled = request.getScheduledAt() != null && !request.getScheduledAt().isBlank();
        java.time.OffsetDateTime scheduledFor = null;

        if (isScheduled) {
            try {
                // Parse ISO 8601 string with timezone (e.g., "2026-05-28T15:00:00+09:00")
                scheduledFor = java.time.OffsetDateTime.parse(request.getScheduledAt());
                
                // Validate it's in the future
                if (!scheduledFor.isAfter(java.time.OffsetDateTime.now())) {
                    throw new BusinessRuleViolationException(
                            "scheduledAt must be a future date/time");
                }
            } catch (java.time.format.DateTimeParseException e) {
                throw new BusinessRuleViolationException(
                        "Invalid scheduledAt format. Expected ISO 8601 with timezone (e.g., 2026-05-28T15:00:00+09:00)");
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
            // Convert OffsetDateTime to UTC LocalDateTime for storage
            java.time.Instant utcInstant = scheduledFor.toInstant();
            LocalDateTime utcLocalDateTime = utcInstant.atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
            service.setScheduledFor(utcLocalDateTime);
            
            if (request.getAgreedHours() != null) {
                service.setScheduledEndAt(utcLocalDateTime.plusHours(request.getAgreedHours()));
            }
        }

        ServiceEntity saved = serviceRepository.save(service);

        // Idempotency key persistence
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ServiceIdempotencyKey row = new ServiceIdempotencyKey();
            row.setClientId(authenticatedClientId);
            row.setIdempotencyKey(idempotencyKey.trim());
            row.setServiceId(saved.getIdService());
            serviceIdempotencyKeyRepository.save(row);
        }

        return serviceMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------
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
        // Scheduled services are visible immediately (status=REQUESTED), so no change needed here.
        List<ServiceEntity> services = serviceRepository.findByStatus(ServiceStatus.REQUESTED);
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

    // -------------------------------------------------------------------------
    // Payment operations
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // acceptService()
    // Immediate:  REQUESTED → ACCEPTED
    // Scheduled:  REQUESTED → WAITING_FOR_START
    // -------------------------------------------------------------------------
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

        // Updated to cover all live partner-side statuses (includes WAITING_FOR_START, READY)
        boolean hasActiveService = serviceRepository.existsByPartner_IdAndStatusIn(
                partnerId, PARTNER_ACTIVE_STATUSES);

        if (hasActiveService) {
            throw new BusinessRuleViolationException("Partner already has an active service");
        }

        service.setPartner(partner);
        service.setAcceptedAt(LocalDateTime.now());

        // Branching: scheduled services hold in WAITING_FOR_START until their time window opens
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

    // -------------------------------------------------------------------------
    // readyService()
    // Immediate:  ACCEPTED          → READY
    // Scheduled:  WAITING_FOR_START → READY
    // -------------------------------------------------------------------------
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

        // Scheduled time-gate: must not mark as ready before the reserved date/time
        if (service.isScheduled()
                && service.getScheduledFor() != null
                && LocalDateTime.now().isBefore(service.getScheduledFor())) {
            throw new BusinessRuleViolationException(
                    "Scheduled service cannot be set to READY before its scheduled time: "
                            + service.getScheduledFor());
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

    // -------------------------------------------------------------------------
    // startService()
    // Both flows now enter from READY (not ACCEPTED).
    // Scheduled services add a time-gate: now >= scheduledFor.
    // -------------------------------------------------------------------------
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

        // Scheduled time-gate: must not start before the reserved date/time
        if (service.isScheduled()
                && service.getScheduledFor() != null
                && LocalDateTime.now().isBefore(service.getScheduledFor())) {
            throw new BusinessRuleViolationException(
                    "Scheduled service cannot be started before its scheduled time: "
                            + service.getScheduledFor());
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

    // -------------------------------------------------------------------------
    // Complete operations
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // cancelService()
    // Added WAITING_FOR_START and READY to the cancellable set.
    // -------------------------------------------------------------------------
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

        // WAITING_FOR_START and READY added alongside the original REQUESTED / ACCEPTED
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

    // -------------------------------------------------------------------------
    // cancelServiceByPartner()
    // Added WAITING_FOR_START and READY to the cancellable set.
    // -------------------------------------------------------------------------
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

        // WAITING_FOR_START and READY added alongside the original ACCEPTED
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