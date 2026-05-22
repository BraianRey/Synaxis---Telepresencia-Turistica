package com.synexis.management_service.services;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.synexis.management_service.service.impl.RatingServiceImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingServiceImpl - Unit Tests")
class RatingServiceImplTest {

    // ─── Repository mocks ─────────────────────────────────────────────────────
    @Mock private RatingRepository    ratingRepository;
    @Mock private ServiceRepository   serviceRepository;
    @Mock private ClientRepository    clientRepository;
    @Mock private PartnerRepository   partnerRepository;

    // ─── Security mocks ───────────────────────────────────────────────────────
    @Mock private SecurityContext  securityContext;
    @Mock private Authentication   authentication;

    @InjectMocks
    private RatingServiceImpl ratingService;

    // ─── Reference IDs ───────────────────────────────────────────────────────
    private static final Long CLIENT_ID  = 1L;
    private static final Long PARTNER_ID = 2L;
    private static final Long SERVICE_ID = 3L;
    private static final Long RATING_ID  = 4L;
    private static final String KEYCLOAK_ID = "31fdeb94-0271-4b77-8dcc-b5c7c9c37bb2";

    // ─── Reusable objects ─────────────────────────────────────────────────────
    private Client        client;
    private Partner       partner;
    private ServiceEntity service;
    private Rating        rating;

    @BeforeEach
    void setUp() {
        client  = buildClient();
        partner = buildPartner();
        service = buildService(client, partner, ServiceStatus.COMPLETED);
        rating  = buildRating(client, partner, service);
    }

    // =========================================================================
    // createRating()
    // =========================================================================

    @Nested
    @DisplayName("createRating()")
    class CreateRating {

        @Test
        @DisplayName("Successful case: creates the rating and recalculates partner average")
        void shouldCreateRatingSuccessfully() {
            // Setup request DTO 
            RatingRequest request = buildRequest(SERVICE_ID, 5, "Excellent service provided");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
            when(ratingRepository.existsByService(service)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenReturn(rating);
            when(ratingRepository.findAverageScoreByPartnerId(PARTNER_ID)).thenReturn(Optional.of(4.5));
            when(ratingRepository.countByPartnerId(PARTNER_ID)).thenReturn(3L);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                // Act
                RatingResponse result = ratingService.createRating(request);

                // Assert — DTO fields
                assertThat(result).isNotNull();
                assertThat(result.getServiceId()).isEqualTo(SERVICE_ID);
                assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
                assertThat(result.getPartnerId()).isEqualTo(PARTNER_ID);
                assertThat(result.getScore()).isEqualTo(5);
                assertThat(result.getComment()).isEqualTo("Excellent service provided");

                // Verify — save invoked exactly once
                verify(ratingRepository, times(1)).save(any(Rating.class));

                // Verify — updated partner stats
                ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
                verify(partnerRepository, times(1)).save(partnerCaptor.capture());
                Partner saved = partnerCaptor.getValue();
                assertThat(saved.getAverageRating()).isEqualTo(4.5);
                assertThat(saved.getRatingCount()).isEqualTo(3);
            }
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the service does not exist")
        void shouldThrowWhenServiceNotFound() {
            RatingRequest request = buildRequest(SERVICE_ID, 4, "Very good service delivered");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.createRating(request))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining(String.valueOf(SERVICE_ID));

                verify(ratingRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the authenticated client does not exist in DB")
        void shouldThrowWhenAuthenticatedClientNotFound() {
            RatingRequest request = buildRequest(SERVICE_ID, 3, "Acceptable service received");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.createRating(request))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining(KEYCLOAK_ID);

                verifyNoInteractions(serviceRepository, ratingRepository, partnerRepository);
            }
        }

        @Test
        @DisplayName("Throws AccessDeniedException when the service does not belong to the authenticated client")
        void shouldThrowWhenServiceDoesNotBelongToClient() {
            Client otherClient = buildClientWithId(99L, "other@test.com");
            ServiceEntity foreignService = buildService(otherClient, partner, ServiceStatus.COMPLETED);
            foreignService.setIdService(SERVICE_ID);

            RatingRequest request = buildRequest(SERVICE_ID, 4, "Well-executed service today");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(foreignService));

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.createRating(request))
                        .isInstanceOf(AccessDeniedException.class);

                verify(ratingRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("Throws BusinessException when the service is not in COMPLETED status")
        void shouldThrowWhenServiceNotCompleted() {
            ServiceEntity pendingService = buildService(client, partner, ServiceStatus.REQUESTED);
            pendingService.setIdService(SERVICE_ID);

            RatingRequest request = buildRequest(SERVICE_ID, 5, "Service completed without issues");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(pendingService));

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.createRating(request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("COMPLETED");

                verify(ratingRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("Throws BusinessException when the service has already been rated")
        void shouldThrowWhenServiceAlreadyRated() {
            RatingRequest request = buildRequest(SERVICE_ID, 4, "Very satisfactory service received");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
            when(ratingRepository.existsByService(service)).thenReturn(true);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.createRating(request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("already been rated");

                verify(ratingRepository, never()).save(any());
            }
        }
    }

    // =========================================================================
    // getRatingById()
    // =========================================================================

    @Nested
    @DisplayName("getRatingById()")
    class GetRatingById {

        @Test
        @DisplayName("Successful case: returns the DTO when the rating exists")
        void shouldReturnRatingWhenFound() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));

            RatingResponse result = ratingService.getRatingById(RATING_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(RATING_ID);
            assertThat(result.getScore()).isEqualTo(rating.getScore());
            assertThat(result.getComment()).isEqualTo(rating.getComment());

            verify(ratingRepository, times(1)).findById(RATING_ID);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the rating does not exist")
        void shouldThrowWhenRatingNotFound() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.getRatingById(RATING_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(RATING_ID));
        }
    }

    // =========================================================================
    // getRatingsByPartner()
    // =========================================================================

    @Nested
    @DisplayName("getRatingsByPartner()")
    class GetRatingsByPartner {

        @Test
        @DisplayName("Returns a list when the partner has ratings")
        void shouldReturnRatingsListWhenPartnerHasRatings() {
            Rating second = buildRating(client, partner, service);
            second.setId(5L);
            second.setScore(3);

            when(ratingRepository.findByPartnerId(PARTNER_ID)).thenReturn(List.of(rating, second));

            List<RatingResponse> result = ratingService.getRatingsByPartner(PARTNER_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(RatingResponse::getPartnerId)
                    .containsOnly(PARTNER_ID);

            verify(ratingRepository, times(1)).findByPartnerId(PARTNER_ID);
        }

        @Test
        @DisplayName("Returns an empty list when the partner has no ratings")
        void shouldReturnEmptyListWhenPartnerHasNoRatings() {
            when(ratingRepository.findByPartnerId(PARTNER_ID)).thenReturn(Collections.emptyList());

            List<RatingResponse> result = ratingService.getRatingsByPartner(PARTNER_ID);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getRatingByService()
    // =========================================================================

    @Nested
    @DisplayName("getRatingByService()")
    class GetRatingByService {

        @Test
        @DisplayName("Successful case: returns the rating associated with the service")
        void shouldReturnRatingWhenServiceHasRating() {
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
            when(ratingRepository.findByService(service)).thenReturn(Optional.of(rating));

            RatingResponse result = ratingService.getRatingByService(SERVICE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getServiceId()).isEqualTo(SERVICE_ID);
            assertThat(result.getScore()).isEqualTo(rating.getScore());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the service has no rating")
        void shouldThrowWhenServiceHasNoRating() {
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
            when(ratingRepository.findByService(service)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.getRatingByService(SERVICE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SERVICE_ID));
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the service does not exist")
        void shouldThrowWhenServiceNotFound() {
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.getRatingByService(SERVICE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SERVICE_ID));

            verifyNoInteractions(ratingRepository);
        }
    }

    // =========================================================================
    // updateRating()
    // =========================================================================

    @Nested
    @DisplayName("updateRating()")
    class UpdateRating {

        @Test
        @DisplayName("Successful case: updates score, comment and recalculates partner average")
        void shouldUpdateRatingSuccessfully() {
            RatingRequest request = buildRequest(SERVICE_ID, 2, "Service could be improved in various aspects");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ratingRepository.findAverageScoreByPartnerId(PARTNER_ID)).thenReturn(Optional.of(3.0));
            when(ratingRepository.countByPartnerId(PARTNER_ID)).thenReturn(5L);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                RatingResponse result = ratingService.updateRating(RATING_ID, request);

                // Assert — updated values reflected
                assertThat(result.getScore()).isEqualTo(2);
                assertThat(result.getComment()).isEqualTo("Service could be improved in various aspects");

                // Verify — rating save
                verify(ratingRepository, times(1)).save(any(Rating.class));

                // Verify — partner recalculation
                ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
                verify(partnerRepository, times(1)).save(captor.capture());
                assertThat(captor.getValue().getAverageRating()).isEqualTo(3.0);
                assertThat(captor.getValue().getRatingCount()).isEqualTo(5);
            }
        }

        @Test
        @DisplayName("Throws AccessDeniedException when the rating does not belong to the authenticated client")
        void shouldThrowWhenRatingDoesNotBelongToClient() {
            Client otherClient = buildClientWithId(99L, "other@test.com");
            Rating foreignRating = buildRating(otherClient, partner, service);
            foreignRating.setId(RATING_ID);

            RatingRequest request = buildRequest(SERVICE_ID, 1, "Terrible service received today");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(foreignRating));

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.updateRating(RATING_ID, request))
                        .isInstanceOf(AccessDeniedException.class);

                verify(ratingRepository, never()).save(any());
                verifyNoInteractions(partnerRepository);
            }
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the rating does not exist")
        void shouldThrowWhenRatingNotFound() {
            RatingRequest request = buildRequest(SERVICE_ID, 4, "Quite satisfactory service received");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.updateRating(RATING_ID, request))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining(String.valueOf(RATING_ID));

                verify(ratingRepository, never()).save(any());
            }
        }
    }

    // =========================================================================
    // deleteRating()
    // =========================================================================

    @Nested
    @DisplayName("deleteRating()")
    class DeleteRating {

        @Test
        @DisplayName("Successful case: deletes the rating and recalculates partner average")
        void shouldDeleteRatingAndUpdatePartnerStats() {
            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));
            when(ratingRepository.findAverageScoreByPartnerId(PARTNER_ID)).thenReturn(Optional.of(4.0));
            when(ratingRepository.countByPartnerId(PARTNER_ID)).thenReturn(2L);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                ratingService.deleteRating(RATING_ID);

                // Verify — actual deletion
                verify(ratingRepository, times(1)).delete(rating);

                // Verify — partner recalculation after deletion
                ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
                verify(partnerRepository, times(1)).save(captor.capture());
                assertThat(captor.getValue().getAverageRating()).isEqualTo(4.0);
                assertThat(captor.getValue().getRatingCount()).isEqualTo(2);
            }
        }

        @Test
        @DisplayName("Throws AccessDeniedException when the rating does not belong to the authenticated client")
        void shouldThrowWhenClientNotOwnerOnDelete() {
            Client otherClient = buildClientWithId(99L, "other@test.com");
            Rating foreignRating = buildRating(otherClient, partner, service);
            foreignRating.setId(RATING_ID);

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(foreignRating));

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.deleteRating(RATING_ID))
                        .isInstanceOf(AccessDeniedException.class);

                verify(ratingRepository, never()).delete(any());
                verifyNoInteractions(partnerRepository);
            }
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the rating to delete does not exist")
        void shouldThrowWhenRatingNotFoundOnDelete() {
            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThatThrownBy(() -> ratingService.deleteRating(RATING_ID))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining(String.valueOf(RATING_ID));

                verify(ratingRepository, never()).delete(any());
            }
        }
    }

    // =========================================================================
    // updatePartnerRatingStats() — tested indirectly via create/update/delete
    // =========================================================================

    @Nested
    @DisplayName("updatePartnerRatingStats() — recalculation behavior")
    class UpdatePartnerRatingStats {

        @Test
        @DisplayName("Assigns correct AVG and COUNT when ratings exist")
        void shouldSetAverageAndCountCorrectly() {
            RatingRequest request = buildRequest(SERVICE_ID, 5, "Impeccable service in every way");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
            when(ratingRepository.existsByService(service)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenReturn(rating);
            when(ratingRepository.findAverageScoreByPartnerId(PARTNER_ID)).thenReturn(Optional.of(4.8));
            when(ratingRepository.countByPartnerId(PARTNER_ID)).thenReturn(10L);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                ratingService.createRating(request);

                ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
                verify(partnerRepository).save(captor.capture());

                assertThat(captor.getValue().getAverageRating()).isEqualTo(4.8);
                assertThat(captor.getValue().getRatingCount()).isEqualTo(10);
            }
        }

        @Test
        @DisplayName("Assigns averageRating = 0.0 when AVG returns Optional.empty() (no ratings)")
        void shouldSetAverageToZeroWhenAvgIsEmpty() {
            RatingRequest request = buildRequest(SERVICE_ID, 5, "First service rated now");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
            when(ratingRepository.existsByService(service)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenReturn(rating);

            // AVG returns empty (no records in DB yet)
            when(ratingRepository.findAverageScoreByPartnerId(PARTNER_ID)).thenReturn(Optional.empty());
            when(ratingRepository.countByPartnerId(PARTNER_ID)).thenReturn(0L);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                ratingService.createRating(request);

                ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
                verify(partnerRepository).save(captor.capture());

                assertThat(captor.getValue().getAverageRating()).isEqualTo(0.0);
                assertThat(captor.getValue().getRatingCount()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("Assigns ratingCount = 0 when COUNT returns 0")
        void shouldSetRatingCountToZeroWhenCountIsZero() {
            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));
            when(ratingRepository.findAverageScoreByPartnerId(PARTNER_ID)).thenReturn(Optional.empty());
            when(ratingRepository.countByPartnerId(PARTNER_ID)).thenReturn(0L);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                ratingService.deleteRating(RATING_ID);

                ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
                verify(partnerRepository).save(captor.capture());

                assertThat(captor.getValue().getRatingCount()).isEqualTo(0);
                assertThat(captor.getValue().getAverageRating()).isEqualTo(0.0);
            }
        }

        @Test
        @DisplayName("Persists the partner with updated values via partnerRepository.save()")
        void shouldPersistPartnerWithUpdatedStats() {
            RatingRequest request = buildRequest(SERVICE_ID, 4, "Service delivered with notable quality");

            mockSecurityContext();
            when(clientRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(client));
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
            when(ratingRepository.existsByService(service)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenReturn(rating);
            when(ratingRepository.findAverageScoreByPartnerId(PARTNER_ID)).thenReturn(Optional.of(3.75));
            when(ratingRepository.countByPartnerId(PARTNER_ID)).thenReturn(4L);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                ratingService.createRating(request);

                // Exactly one call to partnerRepository.save
                verify(partnerRepository, times(1)).save(any(Partner.class));
                verifyNoMoreInteractions(partnerRepository);
            }
        }
    }

    // =========================================================================
    // Private helpers — dummy object construction
    // =========================================================================

    private Client buildClient() {
        return buildClientWithId(CLIENT_ID, KEYCLOAK_ID);
    }

    private Client buildClientWithId(Long id, String keycloakId) {
        Client c = new Client();
        c.setId(id);
        c.setKeycloakId(keycloakId);
        c.setEmail("client@test.com");
        return c;
    }

    private Partner buildPartner() {
        Partner p = new Partner();
        p.setId(PARTNER_ID);
        p.setAverageRating(0.0);
        p.setRatingCount(0);
        return p;
    }

    private ServiceEntity buildService(Client owner, Partner assignedPartner, ServiceStatus status) {
        ServiceEntity s = new ServiceEntity();
        s.setIdService(SERVICE_ID);
        s.setClient(owner);
        s.setPartner(assignedPartner);
        s.setStatus(status);
        return s;
    }

    private Rating buildRating(Client owner, Partner assignedPartner, ServiceEntity linkedService) {
        Rating r = new Rating();
        r.setId(RATING_ID);
        r.setScore(5);
        r.setComment("Excellent service provided");
        r.setClient(owner);
        r.setPartner(assignedPartner);
        r.setService(linkedService);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private RatingRequest buildRequest(Long serviceId, int score, String comment) {
        return RatingRequest.builder()
                .serviceId(serviceId)
                .score(score)
                .comment(comment)
                .build();
    }

    /**
     * Sets up the mock SecurityContext to return the test client's USERNAME.
     * It is combined with MockedStatic<SecurityContextHolder> in each test that requires it.
     */
    private void mockSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(KEYCLOAK_ID);
    }
}
