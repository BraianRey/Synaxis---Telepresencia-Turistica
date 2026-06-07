package com.synexis.management_service.service.impl;

import com.synexis.management_service.dto.request.RatingRequest;
import com.synexis.management_service.dto.response.RatingResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.Rating;
import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServiceStatus;
import com.synexis.management_service.exception.BusinessException;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.repository.RatingRepository;
import com.synexis.management_service.repository.ServiceRepository;
import com.synexis.management_service.service.RatingService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final ServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final PartnerRepository partnerRepository;

    public RatingServiceImpl(
            RatingRepository ratingRepository,
            ServiceRepository serviceRepository,
            ClientRepository clientRepository,
            PartnerRepository partnerRepository
    ) {
        this.ratingRepository = ratingRepository;
        this.serviceRepository = serviceRepository;
        this.clientRepository = clientRepository;
        this.partnerRepository = partnerRepository;
    }

    @Override
    @Transactional
    public RatingResponse createRating(RatingRequest request) {
        Client authenticatedClient = getAuthenticatedClient();

        ServiceEntity service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with ID: " + request.getServiceId()));

        validateServiceOwnership(service, authenticatedClient);
        validateServiceCompleted(service);
        validateNotAlreadyRated(service);

        Rating rating = Rating.builder()
                .score(request.getScore())
                .comment(request.getComment() != null ? request.getComment() : "")
                .client(authenticatedClient)
                .partner(service.getPartner())
                .service(service)
                .build();

        Rating saved = ratingRepository.save(rating);
        updatePartnerRatingStats(service.getPartner());

        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponse getRatingById(Long id) {
        Rating rating = findRatingOrThrow(id);
        return toResponseDTO(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsByPartner(Long partnerId) {
        return ratingRepository.findByPartnerId(partnerId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponse getRatingByService(Long serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with ID: " + serviceId));

        Rating rating = ratingRepository.findByService(service)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service with ID " + serviceId + " has not been rated yet"));

        return toResponseDTO(rating);
    }

    @Override
    @Transactional
    public RatingResponse updateRating(Long id, RatingRequest request) {
        Client authenticatedClient = getAuthenticatedClient();
        Rating rating = findRatingOrThrow(id);

        if (!rating.getClient().getId().equals(authenticatedClient.getId())) {
            throw new AccessDeniedException("You do not have permission to edit this rating");
        }

        rating.setScore(request.getScore());
        rating.setComment(request.getComment() != null ? request.getComment() : "");

        Rating updated = ratingRepository.save(rating);
        updatePartnerRatingStats(rating.getPartner());

        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteRating(Long id) {
        Client authenticatedClient = getAuthenticatedClient();
        Rating rating = findRatingOrThrow(id);

        if (!rating.getClient().getId().equals(authenticatedClient.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this rating");
        }

        Partner partner = rating.getPartner();
        ratingRepository.delete(rating);
        updatePartnerRatingStats(partner);
    }

    // ─── Private methods ────────────────────────────────────────────────────

    /**
     * Retrieves the authenticated Client from the JWT subject in the SecurityContext.
     * The token subject is the Keycloak user UUID (keycloakId).
     */
    private Client getAuthenticatedClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String keycloakId = authentication.getName(); // JWT subject (Keycloak UUID)

        return clientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated client not found: " + keycloakId));
    }

    private void validateServiceOwnership(ServiceEntity service, Client client) {
        if (!service.getClient().getId().equals(client.getId())) {
            throw new AccessDeniedException("Only the service owner client can rate it");
        }
    }

    private void validateServiceCompleted(ServiceEntity service) {
        if (service.getStatus() != ServiceStatus.COMPLETED) {
            throw new BusinessException("The service must be in COMPLETED status to be rated");
        }
    }

    private void validateNotAlreadyRated(ServiceEntity service) {
        if (ratingRepository.existsByService(service)) {
            throw new BusinessException("This service has already been rated and cannot be rated again");
        }
    }

    private RatingResponse toResponseDTO(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .serviceId(rating.getService().getIdService())
                .clientId(rating.getClient().getId())
                .partnerId(rating.getPartner().getId())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    /**
     * Recalculates AVG(score) and COUNT(*) for the Partner using JPQL queries
     * and updates the averageRating and ratingCount fields.
     */
    private void updatePartnerRatingStats(Partner partner) {
        Double average = ratingRepository.findAverageScoreByPartnerId(partner.getId())
                .orElse(0.0);
        Long count = ratingRepository.countByPartnerId(partner.getId());

        partner.setAverageRating(average);
        partner.setRatingCount(count.intValue());

        partnerRepository.save(partner);
    }

    private Rating findRatingOrThrow(Long id) {
        return ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rating not found with ID: " + id));
    }

    
}