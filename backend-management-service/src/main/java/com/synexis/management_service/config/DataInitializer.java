package com.synexis.management_service.config;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServiceStatus;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.repository.ServiceRepository;
import com.synexis.management_service.service.ClientService;
import com.synexis.management_service.service.PartnerService;

import java.time.LocalDateTime;

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
    private final ServiceRepository serviceRepository;

    public DataInitializer(ClientService clientService,
            PartnerService partnerService,
            ClientRepository clientRepository,
            PartnerRepository partnerRepository,
            ServiceRepository serviceRepository) {
        this.clientService = clientService;
        this.partnerService = partnerService;
        this.clientRepository = clientRepository;
        this.partnerRepository = partnerRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("\n\n=== DataInitializer: starting seed ===\n\n");
        seedClients();
        seedPartners();
        seedServices();
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

    // ------------------------------------------------------------------
    // SERVICES
    // ------------------------------------------------------------------
    private void seedServices() {
        // Only seed if no services exist yet
        if (serviceRepository.count() > 0) {
            log.info("Services already exist, skipping seed.");
            return;
        }

        Client ana = clientRepository.findByEmailIgnoreCase("ana.seed@gmail.com").orElse(null);
        Client luis = clientRepository.findByEmailIgnoreCase("luis.seed@gmail.com").orElse(null);
        Client john = clientRepository.findByEmailIgnoreCase("john.seed@gmail.com").orElse(null);

        Partner carlos = partnerRepository.findByEmailIgnoreCase("carlos.seed@gmail.com").orElse(null);
        Partner laura = partnerRepository.findByEmailIgnoreCase("laura.seed@gmail.com").orElse(null);

        if (ana == null || luis == null || john == null || carlos == null || laura == null) {
            log.warn("Seed users not found, skipping service seed.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // COMPLETED — visible in history for both client and partner
        seedService(ana, carlos, "Puente del Humilladero, Popayán",
                2, ServiceStatus.COMPLETED,
                3.4264, -76.5102,
                now.minusDays(10), now.minusDays(10).plusHours(1),
                now.minusDays(10).plusHours(1), now.minusDays(10).plusHours(3));

        seedService(luis, carlos, "Cristo Rey, Cali",
                3, ServiceStatus.COMPLETED,
                6.2105, -75.5677,
                now.minusDays(5), now.minusDays(5).plusHours(1),
                now.minusDays(5).plusHours(2), now.minusDays(5).plusHours(5));

        // CANCELLED — also visible in history
        seedService(john, laura, "Comuna 13, Medellín",
                1, ServiceStatus.CANCELLED,
                6.2518, -75.5636,
                now.minusDays(3), null, null, null);

        // ACCEPTED — visible in active for both client and partner
        seedService(ana, laura, "La Candelaria, Bogotá",
                4, ServiceStatus.ACCEPTED,
                4.5981, -74.0758,
                now.minusHours(2), now.minusHours(1), null, null);

        log.info("Services seeded.");
    }

    private void seedService(Client client, Partner partner,
            String locationDescription, int agreedHours,
            ServiceStatus status,
            double latitude, double longitude,
            LocalDateTime requestedAt, LocalDateTime acceptedAt,
            LocalDateTime startedAt, LocalDateTime endedAt) {
        try {
            ServiceEntity service = new ServiceEntity();
            service.setClient(client);
            service.setPartner(partner);
            service.setStartLocationDescription(locationDescription);
            service.setAgreedHours(agreedHours);
            service.setStatus(status);
            service.setLatitude(latitude);
            service.setLongitude(longitude);
            service.setRequestedAt(requestedAt);
            service.setAcceptedAt(acceptedAt);
            service.setStartedAt(startedAt);
            service.setEndedAt(endedAt);
            serviceRepository.save(service);
            log.info("Service seeded: {} → {} [{}]", client.getId(), partner.getId(), status);
        } catch (Exception e) {
            log.warn("Could not seed service: {}", e.getMessage());
        }
    }
}