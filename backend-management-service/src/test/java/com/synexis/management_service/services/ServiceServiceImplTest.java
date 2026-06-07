package com.synexis.management_service.services;

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
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.repository.ServiceIdempotencyKeyRepository;
import com.synexis.management_service.repository.ServicePaymentRepository;
import com.synexis.management_service.repository.ServiceRepository;
import com.synexis.management_service.service.NotificationService;
import com.synexis.management_service.service.ServiceHistoryService;
import com.synexis.management_service.service.impl.NoopPaymentService;
import com.synexis.management_service.service.impl.ServiceServiceImpl;
import com.synexis.management_service.client.WikimediaClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceImplTest {

    // -------------------------------------------------------------------------
    // Mocks — must match constructor parameters exactly
    // -------------------------------------------------------------------------
    @Mock private ServiceRepository                 serviceRepository;
    @Mock private ServiceMapper                     serviceMapper;
    @Mock private PartnerRepository                 partnerRepository;
    @Mock private ClientRepository                  clientRepository;
    @Mock private NoopPaymentService                paymentService;   // cast target in completeService
    @Mock private ServicePaymentRepository          servicePaymentRepository;
    @Mock private ServiceHistoryService             serviceHistoryService;
    @Mock private NotificationService               notificationService;
    @Mock private WikimediaClient                   wikimediaClient;
    @Mock private ServiceIdempotencyKeyRepository   serviceIdempotencyKeyRepository;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    // =========================================================================
    // Builders
    // =========================================================================

    /**
     * Creates a Mockito mock for Client with stubbed getId() and getStatus().
     * lenient() prevents "unnecessary stubbing" failures when only a subset of
     * methods is exercised in a given test.
     */
    private Client buildClient(Long id, UserStatus status) {
        Client client = mock(Client.class);
        lenient().when(client.getId()).thenReturn(id);
        lenient().when(client.getStatus()).thenReturn(status);
        return client;
    }

    /**
     * Creates a Mockito mock for Partner with stubbed getId(), getStatus() and
     * getAvailabilityStatus(). Uses a real setter for availability so tests that
     * verify status changes (completeService) can observe the mutation.
     */
    private Partner buildPartner(Long id, UserStatus status, PartnerAvailabilityStatus availability) {
        Partner partner = mock(Partner.class);
        lenient().when(partner.getId()).thenReturn(id);
        lenient().when(partner.getStatus()).thenReturn(status);
        lenient().when(partner.getAvailabilityStatus()).thenReturn(availability);
        return partner;
    }

    /** Immediate (non-scheduled) service in the given status. */
    private ServiceEntity buildService(Long id, ServiceStatus status, Client client) {
        ServiceEntity service = new ServiceEntity();
        service.setIdService(id);
        service.setStatus(status);
        service.setClient(client);
        service.setScheduled(false);
        return service;
    }

    /** Scheduled service in the given status with a pre-set scheduledFor time. */
    private ServiceEntity buildScheduledService(Long id, ServiceStatus status,
                                                Client client, LocalDateTime scheduledFor) {
        ServiceEntity service = buildService(id, status, client);
        service.setScheduled(true);
        service.setScheduledFor(scheduledFor);
        return service;
    }

    /** Minimal immediate-service request. */
    private RegisterServiceRequest buildImmediateRequest() {
        RegisterServiceRequest req = new RegisterServiceRequest();
        req.setScheduledAt(null);  // No scheduled time means immediate
        req.setAgreedHours(2);
        req.setLongitude(-76.5);
        req.setLatitude(2.4);
        return req;
    }

    /** Scheduled-service request for the given future time. */
    private RegisterServiceRequest buildScheduledRequest(LocalDateTime scheduledFor) {
        RegisterServiceRequest req = new RegisterServiceRequest();
        // Convert LocalDateTime to OffsetDateTime using system default timezone, then format as ISO string
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        java.time.ZonedDateTime zoned = scheduledFor.atZone(zoneId);
        java.time.OffsetDateTime offsetDateTime = zoned.toOffsetDateTime();
        req.setScheduledAt(offsetDateTime.toString());  // ISO 8601 format with timezone
        req.setAgreedHours(2);
        req.setLongitude(-76.5);
        req.setLatitude(2.4);
        return req;
    }

    // =========================================================================
    // registerService — immediate service
    // =========================================================================

    @Test
    void shouldCreateImmediateServiceSuccessfully() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        RegisterServiceRequest request = buildImmediateRequest();
        ServiceEntity mapped = buildService(null, null, client);
        ServiceEntity saved  = buildService(10L, ServiceStatus.REQUESTED, client);
        ServiceResponse expected = new ServiceResponse();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.existsByClient_IdAndStatusIn(eq(clientId), anySet())).thenReturn(false);
        when(serviceMapper.toEntity(request, client)).thenReturn(mapped);
        when(serviceRepository.save(mapped)).thenReturn(saved);
        when(serviceMapper.toResponse(saved)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.registerService(request, clientId, null);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(mapped.isScheduled()).isFalse();
        assertThat(mapped.getStatus()).isEqualTo(ServiceStatus.REQUESTED);
        assertThat(mapped.getRequestedAt()).isNotNull();
        verify(serviceRepository).save(mapped);
    }

    @Test
    void shouldRejectRegistrationWhenClientIsInactive() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.inactive);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() ->
                serviceService.registerService(buildImmediateRequest(), clientId, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void shouldRejectRegistrationWhenClientAlreadyHasActiveService() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.existsByClient_IdAndStatusIn(eq(clientId), anySet())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                serviceService.registerService(buildImmediateRequest(), clientId, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already have an active service");
    }

    // =========================================================================
    // registerService — scheduled service
    // =========================================================================

    @Test
    void shouldCreateScheduledServiceSuccessfully() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        RegisterServiceRequest request = buildScheduledRequest(tomorrow);
        ServiceEntity mapped = buildService(null, null, client);
        ServiceEntity saved  = buildService(10L, ServiceStatus.REQUESTED, client);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.existsByClient_IdAndStatusIn(eq(clientId), anySet())).thenReturn(false);
        when(serviceMapper.toEntity(request, client)).thenReturn(mapped);
        when(serviceRepository.save(mapped)).thenReturn(saved);
        when(serviceMapper.toResponse(saved)).thenReturn(new ServiceResponse());

        // Act
        serviceService.registerService(request, clientId, null);

        // Assert
        assertThat(mapped.isScheduled()).isTrue();
        assertThat(mapped.getScheduledFor()).isEqualTo(tomorrow);
        assertThat(mapped.getStatus()).isEqualTo(ServiceStatus.REQUESTED);
    }

    @Test
    void shouldCalculateScheduledEndAtAsScheduledForPlusAgreedHours() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        RegisterServiceRequest request = buildScheduledRequest(tomorrow);
        request.setAgreedHours(3);
        ServiceEntity mapped = buildService(null, null, client);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.existsByClient_IdAndStatusIn(eq(clientId), anySet())).thenReturn(false);
        when(serviceMapper.toEntity(request, client)).thenReturn(mapped);
        when(serviceRepository.save(mapped)).thenReturn(buildService(10L, ServiceStatus.REQUESTED, client));
        when(serviceMapper.toResponse(any())).thenReturn(new ServiceResponse());

        // Act
        serviceService.registerService(request, clientId, null);

        // Assert
        assertThat(mapped.getScheduledEndAt()).isEqualTo(tomorrow.plusHours(3));
    }

    @Test
    void shouldRejectScheduledServiceWithInvalidScheduledAtFormat() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        RegisterServiceRequest request = new RegisterServiceRequest();
        request.setScheduledAt("not-a-valid-date");  // Invalid format
        request.setAgreedHours(2);
        request.setLongitude(-76.5);
        request.setLatitude(2.4);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() ->
                serviceService.registerService(request, clientId, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Invalid scheduledAt format");
    }

    @Test
    void shouldRejectScheduledServiceWhenScheduledAtIsInThePast() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        RegisterServiceRequest request = buildScheduledRequest(LocalDateTime.now().minusMinutes(30));

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() ->
                serviceService.registerService(request, clientId, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("future date/time");
    }

    @Test
    void shouldRejectScheduledServiceWhenScheduledAtIsNow() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        // isAfter(now) is false when equal, so "now" must also be rejected
        RegisterServiceRequest request = buildScheduledRequest(LocalDateTime.now().minusNanos(1));

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() ->
                serviceService.registerService(request, clientId, null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // =========================================================================
    // registerService — idempotency
    // =========================================================================

    @Test
    void shouldReturnExistingServiceWhenIdempotencyKeyMatches() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceIdempotencyKey existingRow = new ServiceIdempotencyKey();
        existingRow.setServiceId(42L);

        ServiceEntity existing = buildService(42L, ServiceStatus.REQUESTED, client);
        ServiceResponse expected = new ServiceResponse();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceIdempotencyKeyRepository.findByClientIdAndIdempotencyKey(clientId, "key-abc"))
                .thenReturn(Optional.of(existingRow));
        when(serviceRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(serviceMapper.toResponse(existing)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.registerService(
                buildImmediateRequest(), clientId, "key-abc");

        // Assert
        assertThat(result).isSameAs(expected);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void shouldPersistNewIdempotencyKeyAfterSuccessfulRegistration() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity mapped = buildService(null, null, client);
        ServiceEntity saved  = buildService(99L, ServiceStatus.REQUESTED, client);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceIdempotencyKeyRepository.findByClientIdAndIdempotencyKey(clientId, "key-xyz"))
                .thenReturn(Optional.empty());
        when(serviceRepository.existsByClient_IdAndStatusIn(eq(clientId), anySet())).thenReturn(false);
        when(serviceMapper.toEntity(any(), eq(client))).thenReturn(mapped);
        when(serviceRepository.save(mapped)).thenReturn(saved);
        when(serviceMapper.toResponse(saved)).thenReturn(new ServiceResponse());

        // Act
        serviceService.registerService(buildImmediateRequest(), clientId, "key-xyz");

        // Assert
        ArgumentCaptor<ServiceIdempotencyKey> captor =
                ArgumentCaptor.forClass(ServiceIdempotencyKey.class);
        verify(serviceIdempotencyKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(clientId);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("key-xyz");
        assertThat(captor.getValue().getServiceId()).isEqualTo(99L);
    }

    @Test
    void shouldRejectIdempotencyKeyExceedingMaxLength() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        String tooLongKey = "x".repeat(129);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() ->
                serviceService.registerService(buildImmediateRequest(), clientId, tooLongKey))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("128 characters");
    }

    // =========================================================================
    // acceptService
    // =========================================================================

    @Test
    void shouldAcceptImmediateServiceAndTransitionToAccepted() {
        // Arrange
        Long partnerId = 2L;
        Client client  = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.REQUESTED, client);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(serviceRepository.existsByPartner_IdAndStatusIn(eq(partnerId), anySet())).thenReturn(false);
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.acceptService(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.ACCEPTED);
        assertThat(service.getAcceptedAt()).isNotNull();
        assertThat(service.getPartner()).isSameAs(partner);
    }

    @Test
    void shouldAcceptScheduledServiceAndTransitionToWaitingForStart() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildScheduledService(
                5L, ServiceStatus.REQUESTED, client, LocalDateTime.now().plusDays(1));
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(serviceRepository.existsByPartner_IdAndStatusIn(eq(partnerId), anySet())).thenReturn(false);
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.acceptService(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.WAITING_FOR_START);
        assertThat(service.getAcceptedAt()).isNotNull();
    }

    @Test
    void shouldRejectAcceptServiceWhenStatusIsNotRequested() {
        // Arrange
        Client client = buildClient(1L, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.acceptService(5L, 2L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only REQUESTED services can be accepted");
    }

    @Test
    void shouldRejectAcceptServiceForInactivePartner() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.inactive, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.REQUESTED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.acceptService(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void shouldRejectAcceptServiceForUnavailablePartner() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.busy);
        ServiceEntity service = buildService(5L, ServiceStatus.REQUESTED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.acceptService(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void shouldRejectAcceptServiceWhenPartnerAlreadyHasActiveService() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.REQUESTED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(serviceRepository.existsByPartner_IdAndStatusIn(eq(partnerId), anySet())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> serviceService.acceptService(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already has an active service");
    }

    @Test
    void shouldRecordHistoryEventWhenServiceIsAccepted() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.REQUESTED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(serviceRepository.existsByPartner_IdAndStatusIn(eq(partnerId), anySet())).thenReturn(false);
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(new ServiceResponse());

        // Act
        serviceService.acceptService(5L, partnerId);

        // Assert
        verify(serviceHistoryService).recordEvent(
                eq(service), eq("PARTNER"), eq(partnerId),
                eq("Service accepted by partner"), any(Instant.class));
    }

    // =========================================================================
    // readyService
    // =========================================================================

    @Test
    void shouldTransitionImmediateServiceFromAcceptedToReady() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);
        service.setPartner(partner);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.readyService(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.READY);
        verify(notificationService).notifyClientServiceReady(service);
    }

    @Test
    void shouldTransitionScheduledServiceFromWaitingForStartToReady() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildScheduledService(
                5L, ServiceStatus.WAITING_FOR_START, client, LocalDateTime.now().plusHours(1));
        service.setPartner(partner);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.readyService(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.READY);
        verify(notificationService).notifyClientServiceReady(service);
    }

    @Test
    void shouldRejectReadyServiceForInvalidState() {
        // Arrange
        Client client = buildClient(1L, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.REQUESTED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.readyService(5L, 2L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("ACCEPTED or WAITING_FOR_START");
    }

    @Test
    void shouldRejectReadyServiceWhenPartnerDoesNotOwnService() {
        // Arrange
        Long partnerId      = 2L;
        Long otherPartnerId = 99L;
        Client client          = buildClient(1L, UserStatus.active);
        Partner assignedPartner = buildPartner(otherPartnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);
        service.setPartner(assignedPartner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.readyService(5L, partnerId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessageContaining("does not own");
    }

    @Test
    void shouldRecordHistoryEventWhenServiceSetToReady() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(new ServiceResponse());

        // Act
        serviceService.readyService(5L, partnerId);

        // Assert
        verify(serviceHistoryService).recordEvent(
                eq(service), eq("PARTNER"), eq(partnerId),
                eq("Service set to READY by partner"), any(Instant.class));
    }

    // =========================================================================
    // startService
    // =========================================================================

    @Test
    void shouldStartReadyImmediateServiceSuccessfully() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.READY, client);
        service.setPartner(partner);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.startService(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.IN_PROGRESS);
        assertThat(service.getStartedAt()).isNotNull();
    }

    @Test
    void shouldStartScheduledServiceWhenScheduledTimeHasPassed() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildScheduledService(
                5L, ServiceStatus.READY, client, LocalDateTime.now().minusMinutes(1));
        service.setPartner(partner);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.startService(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.IN_PROGRESS);
    }

    @Test
    void shouldRejectStartScheduledServiceBeforeScheduledTime() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildScheduledService(
                5L, ServiceStatus.READY, client, LocalDateTime.now().plusHours(2));
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.startService(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot be started before its scheduled time");
    }

    @Test
    void shouldRejectStartServiceWhenStatusIsNotReady() {
        // Arrange
        Client client = buildClient(1L, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.startService(5L, 2L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only READY services can be started");
    }

    @Test
    void shouldRejectStartServiceWhenPartnerDoesNotOwnService() {
        // Arrange
        Long partnerId      = 2L;
        Long otherPartnerId = 99L;
        Client client          = buildClient(1L, UserStatus.active);
        Partner assignedPartner = buildPartner(otherPartnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.READY, client);
        service.setPartner(assignedPartner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.startService(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not assigned to this partner");
    }

    @Test
    void shouldRejectStartServiceForInactivePartner() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.inactive, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.READY, client);
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.startService(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not active");
    }

    // =========================================================================
    // cancelService — client
    // =========================================================================

    @Test
    void shouldCancelRequestedServiceByClientSuccessfully() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.REQUESTED, client);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.cancelService(5L, clientId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.CANCELLED);
        assertThat(service.getEndedAt()).isNotNull();
        assertThat(service.getPartner()).isNull();
    }

    @Test
    void shouldCancelWaitingForStartServiceByClientSuccessfully() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildScheduledService(
                5L, ServiceStatus.WAITING_FOR_START, client, LocalDateTime.now().plusDays(1));
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.cancelService(5L, clientId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.CANCELLED);
    }

    @Test
    void shouldCancelReadyServiceByClientSuccessfully() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.READY, client);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.cancelService(5L, clientId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.CANCELLED);
    }

    @Test
    void shouldRejectClientCancellationOfInProgressService() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.cancelService(5L, clientId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("In-progress services can only be cancelled by the system");
    }

    @Test
    void shouldRejectClientCancellationOfCompletedService() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.COMPLETED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.cancelService(5L, clientId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Completed or cancelled");
    }

    @Test
    void shouldRejectClientCancellationOfAlreadyCancelledService() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.CANCELLED, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.cancelService(5L, clientId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Completed or cancelled");
    }

    @Test
    void shouldFreePartnerAvailabilityWhenCancellingAcceptedService() {
        // Arrange
        Long clientId  = 1L;
        Long partnerId = 2L;
        Client client   = buildClient(clientId, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.busy);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(new ServiceResponse());

        // Act
        serviceService.cancelService(5L, clientId);

        // Assert
        verify(partner).setAvailabilityStatus(PartnerAvailabilityStatus.available);
        verify(partnerRepository).save(partner);
    }

    // =========================================================================
    // cancelServiceByPartner
    // =========================================================================

    @Test
    void shouldCancelAcceptedServiceByPartnerSuccessfully() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);
        service.setPartner(partner);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.cancelServiceByPartner(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.CANCELLED);
        assertThat(service.getEndedAt()).isNotNull();
    }

    @Test
    void shouldCancelWaitingForStartServiceByPartnerSuccessfully() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildScheduledService(
                5L, ServiceStatus.WAITING_FOR_START, client, LocalDateTime.now().plusDays(1));
        service.setPartner(partner);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.cancelServiceByPartner(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.CANCELLED);
    }

    @Test
    void shouldCancelReadyServiceByPartnerSuccessfully() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.READY, client);
        service.setPartner(partner);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.cancelServiceByPartner(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.CANCELLED);
    }

    @Test
    void shouldRejectPartnerCancellationOfInProgressService() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.cancelServiceByPartner(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot be cancelled by the partner");
    }

    @Test
    void shouldRejectPartnerCancellationWhenPartnerDoesNotOwnService() {
        // Arrange
        Long partnerId      = 2L;
        Long otherPartnerId = 99L;
        Client client          = buildClient(1L, UserStatus.active);
        Partner assignedPartner = buildPartner(otherPartnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);
        service.setPartner(assignedPartner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.cancelServiceByPartner(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not assigned to this partner");
    }

    @Test
    void shouldNotifyClientWhenPartnerCancelsService() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.available);
        ServiceEntity service = buildService(5L, ServiceStatus.ACCEPTED, client);
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(new ServiceResponse());

        // Act
        serviceService.cancelServiceByPartner(5L, partnerId);

        // Assert
        verify(notificationService).notifyClientServiceCancelledByPartner(service);
    }

    // =========================================================================
    // completeService — partner
    // =========================================================================

    @Test
    void shouldCompleteInProgressServiceByPartnerSuccessfully() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.busy);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);
        service.setPartner(partner);
        service.setLatitude(2.4);
        service.setLongitude(-76.5);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(wikimediaClient.getLocationImageUrl(2.4, -76.5)).thenReturn("http://img.example");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.completeService(5L, partnerId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.COMPLETED);
        assertThat(service.getEndedAt()).isNotNull();
        assertThat(service.getLocationReferenceImageUrl()).isEqualTo("http://img.example");
        verify(paymentService).calculateAndPersist(service);
        verify(partner).setAvailabilityStatus(PartnerAvailabilityStatus.available);
        verify(partnerRepository).save(partner);
    }

    @Test
    void shouldRejectCompleteServiceByPartnerWhenNotInProgress() {
        // Arrange
        Client client = buildClient(1L, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.READY, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.completeService(5L, 2L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only IN_PROGRESS services can be completed");
    }

    @Test
    void shouldRejectCompleteServiceByPartnerWhoDoesNotOwnIt() {
        // Arrange
        Long partnerId      = 2L;
        Long otherPartnerId = 99L;
        Client client          = buildClient(1L, UserStatus.active);
        Partner assignedPartner = buildPartner(otherPartnerId, UserStatus.active, PartnerAvailabilityStatus.busy);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);
        service.setPartner(assignedPartner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.completeService(5L, partnerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not assigned to this partner");
    }

    @Test
    void shouldRecordHistoryEventWhenServiceCompletedByPartner() {
        // Arrange
        Long partnerId = 2L;
        Client client   = buildClient(1L, UserStatus.active);
        Partner partner = buildPartner(partnerId, UserStatus.active, PartnerAvailabilityStatus.busy);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(wikimediaClient.getLocationImageUrl(anyDouble(), anyDouble())).thenReturn("http://img");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(new ServiceResponse());

        // Act
        serviceService.completeService(5L, partnerId);

        // Assert
        verify(serviceHistoryService).recordEvent(
                eq(service), eq("PARTNER"), eq(partnerId),
                eq("Service completed"), any(Instant.class));
    }

    // =========================================================================
    // completeServiceByClient
    // =========================================================================

    @Test
    void shouldCompleteInProgressServiceByClientSuccessfully() {
        // Arrange
        Long clientId = 1L;
        Client client   = buildClient(clientId, UserStatus.active);
        Partner partner = buildPartner(2L, UserStatus.active, PartnerAvailabilityStatus.busy);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);
        service.setPartner(partner);
        service.setLatitude(2.4);
        service.setLongitude(-76.5);
        ServiceResponse expected = new ServiceResponse();

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(wikimediaClient.getLocationImageUrl(2.4, -76.5)).thenReturn("http://img.example");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(expected);

        // Act
        ServiceResponse result = serviceService.completeServiceByClient(5L, clientId);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.COMPLETED);
        assertThat(service.getEndedAt()).isNotNull();
        assertThat(service.getLocationReferenceImageUrl()).isEqualTo("http://img.example");
        verify(paymentService).calculateAndPersist(service);
    }

    @Test
    void shouldRejectCompleteByClientWhenNotInProgress() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.READY, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.completeServiceByClient(5L, clientId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only IN_PROGRESS services can be completed");
    }

    @Test
    void shouldRejectCompleteByClientWhoDoesNotOwnService() {
        // Arrange
        Long clientId      = 1L;
        Long otherClientId = 99L;
        Client assignedClient = buildClient(otherClientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, assignedClient);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> serviceService.completeServiceByClient(5L, clientId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessageContaining("not the owner");
    }

    @Test
    void shouldFreePartnerAvailabilityWhenServiceCompletedByClient() {
        // Arrange
        Long clientId = 1L;
        Client client   = buildClient(clientId, UserStatus.active);
        Partner partner = buildPartner(2L, UserStatus.active, PartnerAvailabilityStatus.busy);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);
        service.setPartner(partner);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(wikimediaClient.getLocationImageUrl(anyDouble(), anyDouble())).thenReturn("http://img");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(new ServiceResponse());

        // Act
        serviceService.completeServiceByClient(5L, clientId);

        // Assert
        verify(partner).setAvailabilityStatus(PartnerAvailabilityStatus.available);
        verify(partnerRepository).save(partner);
    }

    @Test
    void shouldRecordHistoryEventWhenServiceCompletedByClient() {
        // Arrange
        Long clientId = 1L;
        Client client = buildClient(clientId, UserStatus.active);
        ServiceEntity service = buildService(5L, ServiceStatus.IN_PROGRESS, client);

        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(wikimediaClient.getLocationImageUrl(anyDouble(), anyDouble())).thenReturn("http://img");
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toResponse(service)).thenReturn(new ServiceResponse());

        // Act
        serviceService.completeServiceByClient(5L, clientId);

        // Assert
        verify(serviceHistoryService).recordEvent(
                eq(service), eq("CLIENT"), eq(clientId),
                eq("Service completed by client"), any(Instant.class));
    }
}