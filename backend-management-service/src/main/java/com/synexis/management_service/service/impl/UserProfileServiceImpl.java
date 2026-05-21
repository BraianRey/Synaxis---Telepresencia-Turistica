package com.synexis.management_service.service.impl;

import com.synexis.management_service.client.NominatimClient;
import com.synexis.management_service.dto.response.usersProfile.ClientProfileResponse;
import com.synexis.management_service.dto.response.usersProfile.PartnerProfileResponse;
import com.synexis.management_service.dto.response.usersProfile.UserProfileResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.UserProfileService;

import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service for user profile operations.
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final ClientRepository clientRepository;
    private final PartnerRepository partnerRepository;
    private final NominatimClient nominatimClient;

    public UserProfileServiceImpl(
            ClientRepository clientRepository,
            PartnerRepository partnerRepository,
            NominatimClient nominatimClient
    ) {
        this.clientRepository = clientRepository;
        this.partnerRepository = partnerRepository;
        this.nominatimClient = nominatimClient;
    }

    @Override
    public UserProfileResponse getMyProfile(String keycloakId) {

        Optional<Client> clientOpt =
                clientRepository.findByKeycloakId(keycloakId);

        if (clientOpt.isPresent()) {

            Client client = clientOpt.get();

            ClientProfileResponse profile =
                    new ClientProfileResponse(
                            client.getName(),
                            client.getEmail(),
                            client.getLanguage().name(),
                            client.getPicDirectory(),
                            client.getCreatedAt()
                    );

            return new UserProfileResponse(
                    client.getRole().name(),
                    profile
            );
        }

        Optional<Partner> partnerOpt =
                partnerRepository.findByKeycloakId(keycloakId);

        if (partnerOpt.isPresent()) {

            Partner partner = partnerOpt.get();

            PartnerProfileResponse profile =
                    new PartnerProfileResponse(
                            partner.getName(),
                            partner.getEmail(),
                            partner.getLanguage().name(),
                            partner.getPicDirectory(),
                            partner.getAverageRating(),
                            partner.getRatingCount(),
                            partner.getAvailabilityStatus().name(),
                            partner.getCreatedAt(),
                            nominatimClient.getCityFromCoordinates(
                                    partner.getLocation().getX(),
                                    partner.getLocation().getY()
                            )
                    );
                    
            return new UserProfileResponse(
                    partner.getRole().name(),
                    profile
            );
        }

        throw new ResourceNotFoundException(
                "User not found with keycloakId: " + keycloakId
        );
    }
}