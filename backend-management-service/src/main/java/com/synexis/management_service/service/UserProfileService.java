package com.synexis.management_service.service;

import com.synexis.management_service.dto.response.UserProfileResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service for user profile operations.
 */
@Service
public class UserProfileService {

    private final ClientRepository clientRepository;
    private final PartnerRepository partnerRepository;

    public UserProfileService(ClientRepository clientRepository, PartnerRepository partnerRepository) {
        this.clientRepository = clientRepository;
        this.partnerRepository = partnerRepository;
    }

    public UserProfileResponse getMyProfile(String keycloakId) {
        // Busca primero en Client
        Optional<Client> clientOpt = clientRepository.findByKeycloakId(keycloakId);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            return new UserProfileResponse(
                    client.getName(),
                    client.getEmail(),
                    client.getStatus().name(),
                    client.getLanguage().name(),
                    client.getRole().name(),
                    client.getPicDirectory());
        }

        // Si no, busca en Partner
        Optional<Partner> partnerOpt = partnerRepository.findByKeycloakId(keycloakId);
        if (partnerOpt.isPresent()) {
            Partner partner = partnerOpt.get();
            return new UserProfileResponse(
                    partner.getName(),
                    partner.getEmail(),
                    partner.getStatus().name(),
                    partner.getLanguage().name(),
                    partner.getRole().name(),
                    partner.getPicDirectory());
        }

        // Si no encuentra en ninguno, lanza excepción
        throw new ResourceNotFoundException("User not found with keycloakId: " + keycloakId);
    }
}