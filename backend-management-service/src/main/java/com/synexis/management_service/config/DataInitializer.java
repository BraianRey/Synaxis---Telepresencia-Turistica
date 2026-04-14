package com.synexis.management_service.config;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.ClientService;
import com.synexis.management_service.service.PartnerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds initial data into Keycloak and PostgreSQL on startup.
 * Skips insertion if the record already exists to support restarts safely.
 * Only runs when the 'dev' profile is active.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ClientService clientService;
    private final PartnerService partnerService;
    private final ClientRepository clientRepository;
    private final PartnerRepository partnerRepository;

    public DataInitializer(ClientService clientService,
            PartnerService partnerService,
            ClientRepository clientRepository,
            PartnerRepository partnerRepository) {
        this.clientService = clientService;
        this.partnerService = partnerService;
        this.clientRepository = clientRepository;
        this.partnerRepository = partnerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("\n\n=== DataInitializer: starting seed ===\n\n");
        seedClients();
        seedPartners();
        log.info("\n\n=== DataInitializer: seed complete ===\n\n");
    }

    // ------------------------------------------------------------------
    // CLIENTS
    // ------------------------------------------------------------------
    private void seedClients() {
        seedClient("Ana Torres", "ana.seed@gmail.com", "password12", UserLanguage.es);
        seedClient("Luis Pérez", "luis.seed@gmail.com", "password12", UserLanguage.es);
        seedClient("John Smith", "john.seed@gmail.com", "password12", UserLanguage.en);
        seedClient("María López", "maria.seed@gmail.com", "password12", UserLanguage.es);
    }

    private void seedClient(String name, String email, String password, UserLanguage language) {
        if (clientRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.info("Client already exists, skipping: {}", email);
            return;
        }
        try {
            clientService.registerClient(new RegisterClientRequest(
                    email, password, name, true, language, null));
            log.info("Client seeded: {}", email);
        } catch (Exception e) {
            log.warn("Could not seed client {}: {}", email, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // PARTNERS
    // ------------------------------------------------------------------
    private void seedPartners() {
        seedPartner("Carlos Guía", "carlos.seed@gmail.com", "password12", 3.4264923857971477, -76.51027679495554,
                UserLanguage.es);
        seedPartner("Laura Viajes", "laura.seed@gmail.com", "password12", 6.2105754412572605, -75.56777001137677,
                UserLanguage.es);
        seedPartner("Pedro Tours", "pedro.seed@gmail.com", "password12", -1.244937522470218, -78.62342836431088,
                UserLanguage.es);
        seedPartner("Sofia Explora", "sofia.seed@gmail.com", "password12", 22.166251011084302, -100.97583711187319,
                UserLanguage.en);
    }

    private void seedPartner(String name, String email, String password,
            Double latitude, Double longitude, UserLanguage language) {
        if (partnerRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.info("Partner already exists, skipping: {}", email);
            return;
        }
        try {
            partnerService.registerPartner(new RegisterPartnerRequest(
                    email, password, name, longitude, latitude, true, language, null));
            log.info("Partner seeded: {}", email);
        } catch (Exception e) {
            log.warn("Could not seed partner {}: {}", email, e.getMessage());
        }
    }
}