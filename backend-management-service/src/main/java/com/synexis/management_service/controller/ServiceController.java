package com.synexis.management_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.synexis.management_service.dto.request.RegisterServiceRequest;
import com.synexis.management_service.dto.response.ServiceResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.ServiceService;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;
    private final PartnerRepository partnerRepository;
    private final ClientRepository clientRepository;

    public ServiceController(ServiceService serviceService,
            PartnerRepository partnerRepository,
            ClientRepository clientRepository) {
        this.serviceService = serviceService;
        this.partnerRepository = partnerRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Creates a new service request on behalf of the authenticated client.
     *
     * <p>Business flow: the service is created in {@code REQUESTED} status and
     * becomes visible to available partners so they can accept it.</p>
     *
     * <p>The client identity comes from the JWT (not from the request body). Optional
     * header {@code X-Idempotency-Key} makes repeated creates return the same service.</p>
     */
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceResponse createService(
            @Valid @RequestBody RegisterServiceRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        String keycloakId = extractKeycloakId(authentication);
        Client client = clientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found for current user"));
        return serviceService.registerService(request, client.getId(), idempotencyKey);
    }

    /**
     * Returns all services that belong to the authenticated client.
     *
     * <p>Used mainly by the client application to render the client's service
     * history and current active requests, regardless of status.</p>
     */
    @GetMapping("/client/{clientId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('CLIENT')")
    public List<ServiceResponse> getServicesByClientId(@PathVariable Long clientId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        Client client = clientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found for current user"));
        return serviceService.getServicesByClientIdForUser(clientId, client.getId());
    }

    /**
     * Returns all services assigned to a given partner.
     *
     * <p>Partner UIs can use this to list current and past services in any
     * status (for example, {@code ACCEPTED}, {@code STARTED}, {@code COMPLETED},
     * {@code CANCELLED}).</p>
     */
    @GetMapping("/partner/{partnerId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public List<ServiceResponse> getServicesByPartnerId(@PathVariable Long partnerId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        Partner partner = partnerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found for current user"));
        return serviceService.getServicesByPartnerIdForUser(partnerId, partner.getId());
    }

    /**
     * Lists all services in {@code REQUESTED} status.
     *
     * <p>Partner frontends call this endpoint to discover services
     * that are still available to be accepted.</p>
     */
    @GetMapping("/available")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public List<ServiceResponse> getAvailableServices(Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        partnerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found for current user"));
        return serviceService.getAvailableServices();
    }

    /**
     * Fetches a single service by id for either client or partner.
     *
     * <p>This is the canonical way for frontend to refresh the latest status and
     * timestamps of a given service.</p>
     */
    @GetMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENT', 'PARTNER')")
    public ServiceResponse getServicesByServiceId(@PathVariable Long serviceId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        return clientRepository.findByKeycloakId(keycloakId)
                .map(c -> serviceService.getServiceForClient(serviceId, c.getId()))
                .orElseGet(() -> partnerRepository.findByKeycloakId(keycloakId)
                        .map(p -> serviceService.getServiceForPartner(serviceId, p.getId()))
                        .orElseThrow(() -> new ResourceNotFoundException("User not found for current user")));
    }

    /**
     * Acceptance endpoint for partners.
     *
     * <p>Flow: a partner that is {@code available} calls
     * {@code POST /api/services/{id}/accept}. If the service is in
     * {@code REQUESTED} status and the partner has no other active service, the
     * backend transitions it to {@code ACCEPTED}, assigns the partner and records
     * the {@code acceptedAt} timestamp and history.</p>
     */
    @PostMapping("/{serviceId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public ServiceResponse acceptService(@PathVariable Long serviceId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        Partner partner = partnerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found for current user"));
        return serviceService.acceptService(serviceId, partner.getId());
    }

    /**
     * Start endpoint for partners.
     *
     * <p>Flow: the same partner that accepted the service calls
     * {@code POST /api/services/{id}/start}. Only services in
     * {@code ACCEPTED} status can be started; on success the service moves to
     * {@code STARTED} and {@code startedAt} is populated.</p>
     */
    @PostMapping("/{serviceId}/start")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public ServiceResponse startService(@PathVariable Long serviceId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        Partner partner = partnerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found for current user"));
        return serviceService.startService(serviceId, partner.getId());
    }

    /**
     * Complete endpoint for partners.
     *
     * <p>Flow: the partner that started the service calls
     * {@code POST /api/services/{id}/complete}. Only services in
     * {@code STARTED} status can be completed; on success the service moves to
     * {@code COMPLETED}, {@code endedAt} is set and the partner is freed if it
     * was marked as {@code busy}.</p>
     */
    @PostMapping("/{serviceId}/complete")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public ServiceResponse completeService(@PathVariable Long serviceId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        Partner partner = partnerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found for current user"));
        return serviceService.completeService(serviceId, partner.getId());
    }

    /**
     * Cancel endpoint for clients.
     *
     * <p>Flow: the owning client can cancel a service via
     * {@code POST /api/services/{id}/cancel} while it is still in
     * {@code REQUESTED} or {@code ACCEPTED} status. The backend validates
     * ownership, cancels the payment pre-authorization, releases the partner if
     * needed and moves the service to {@code CANCELLED}.</p>
     *
     * <p>In-progress services ({@code STARTED}) are not cancelled here but only
     * by the system in case of connection failures.</p>
     */
    @PostMapping("/{serviceId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceResponse cancelService(@PathVariable Long serviceId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        Client client = clientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found for current user"));
        return serviceService.cancelService(serviceId, client.getId());
    }

    /**
     * Cancel endpoint for partners.
     *
     * <p>Flow: the assigned partner may cancel a service via
     * {@code POST /api/services/{id}/cancel/by-partner} while it is still in
     * {@code ACCEPTED} status. The backend validates that the authenticated
     * partner matches the one assigned to the service and then moves the service
     * to {@code CANCELLED}, notifies the client and records the event in the
     * history log.</p>
     *
     * <p>Services in {@code STARTED} status cannot be cancelled from this
     * endpoint.</p>
     */
    @PostMapping("/{serviceId}/cancel/by-partner")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PARTNER')")
    public ServiceResponse cancelServiceByPartner(@PathVariable Long serviceId, Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        Partner partner = partnerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found for current user"));
        return serviceService.cancelServiceByPartner(serviceId, partner.getId());
    }

    /**
     * Extracts the authenticated user's Keycloak subject identifier.
     *
     * <p>For JWT-based security, the {@code sub} claim of the access token is
     * used as the {@code keycloakId}. Controllers then map this identifier to a
     * {@link Client} or {@link Partner} entity via the corresponding
     * repositories.</p>
     */
    private String extractKeycloakId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        return authentication.getName();
    }
}
