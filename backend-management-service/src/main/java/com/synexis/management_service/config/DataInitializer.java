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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    // SE ELIMINÓ @Transactional de aquí para evitar que un fallo aislado tumbe todo
    // el proceso
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedClient(String name, String email, String password, UserLanguage language) {
        if (clientRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.info("Client already exists in DB, skipping: {}", email);
            return;
        }
        try {
            clientService.registerClient(new RegisterClientRequest(
                    email, password, name, true, language, null));
            log.info("Client seeded: {}", email);
        } catch (Exception e) {
            log.warn("Could not seed client {} (it might already exist in Keycloak): {}", email, e.getMessage());
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedPartner(String name, String email, String password,
            Double latitude, Double longitude, UserLanguage language) {
        if (partnerRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.info("Partner already exists in DB, skipping: {}", email);
            return;
        }
        try {
            partnerService.registerPartner(new RegisterPartnerRequest(
                    email, password, name, longitude, latitude, true, language, null));
            log.info("Partner seeded: {}", email);
        } catch (Exception e) {
            log.warn("Could not seed partner {} (it might already exist in Keycloak): {}", email, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // SERVICES
    // ------------------------------------------------------------------
    private void seedServices() {
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
            log.warn("Seed users not found in DB, skipping service seed.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // COMPLETED
        seedService(ana, carlos, "Puente del Humilladero, Popayán", 2, ServiceStatus.COMPLETED, 3.4264, -76.5102,
                now.minusDays(10), now.minusDays(10).plusHours(1), now.minusDays(10).plusHours(1),
                now.minusDays(10).plusHours(3),
                "https://upload.wikimedia.org/wikipedia/commons/f/fc/Puente_del_Humilladero%2C_Popay%C3%A1n_01.jpg");

        // CANCELLED
        seedService(luis, carlos, "Cristo Rey, Cali", 3, ServiceStatus.COMPLETED, 6.2105, -75.5677,
                now.minusDays(5), now.minusDays(5).plusHours(1), now.minusDays(5).plusHours(2),
                now.minusDays(5).plusHours(5),
                "https://upload.wikimedia.org/wikipedia/commons/5/5b/Cristo_Rey%2C_Cali.JPG");

        seedService(john, laura, "Comuna 13, Medellín", 1, ServiceStatus.CANCELLED, 6.2518, -75.5636,
                LocalDateTime.of(now.getYear(), 5, 5, 9, 0), LocalDateTime.of(now.getYear(), 5, 5, 10, 0),
                LocalDateTime.of(now.getYear(), 5, 5, 11, 0), LocalDateTime.of(now.getYear(), 5, 5, 13, 0),
                "https://upload.wikimedia.org/wikipedia/commons/0/00/Ball_court_-_Comuna_13_-_Medell%C3%ADn_-_Colombia_2024.jpg");

        // ACCEPTED
        seedService(ana, laura, "La Candelaria, Bogotá", 4, ServiceStatus.ACCEPTED, 4.5981, -74.0758,
                LocalDateTime.of(now.getYear(), 5, 10, 8, 0), LocalDateTime.of(now.getYear(), 5, 10, 9, 0), null, null,
                "https://upload.wikimedia.org/wikipedia/commons/d/d1/Iglesia_de_Nuestra_Se%C3%B1ora_de_la_Candelaria%2C_Bogota.jpg");

        seedService(ana, laura, "Wall Street, New York", 4, ServiceStatus.COMPLETED, 40.7069, -74.0112,
                LocalDateTime.of(now.getYear(), 9, 10, 8, 0), LocalDateTime.of(now.getYear(), 9, 10, 9, 0),
                LocalDateTime.of(now.getYear(), 9, 10, 9, 30), LocalDateTime.of(now.getYear(), 9, 10, 12, 30),
                "https://upload.wikimedia.org/wikipedia/commons/9/92/New_York_Stock_Exchange_August_2017_02.jpg");

        // ACTIVE SERVICES
        seedService(ana, laura, "Museo Nacional, Bogotá", 4, ServiceStatus.ACCEPTED, 4.5987, -74.0750,
                LocalDateTime.of(now.getYear(), 5, 10, 8, 0), LocalDateTime.of(now.getYear(), 5, 10, 9, 0), null, null,
                "https://upload.wikimedia.org/wikipedia/commons/0/0e/Museo_Nacional_%28Bogot%C3%A1%29_45.jpg");

        seedService(john, carlos, "Museo Botero, Bogotá", 2, ServiceStatus.WAITING_FOR_START, 4.5983, -74.0721,
                now.minusHours(2), now.minusHours(1), null, null,
                "https://upload.wikimedia.org/wikipedia/commons/9/9f/Museo_Botero_-_9_-_Bogot%C3%A1.jpg");

        seedService(luis, laura, "Jardín Botánico, Medellín", 3, ServiceStatus.READY, 6.2450, -75.5792,
                now.minusHours(1), now.minusMinutes(30), now.minusMinutes(15), null,
                "https://upload.wikimedia.org/wikipedia/commons/2/2a/Jardin_Botanico_de_Medellin-Edificio_Cientifico-Interior2.JPG");

        seedService(ana, carlos, "Cerro de Monserrate, Bogotá", 3, ServiceStatus.IN_PROGRESS, 4.6097, -74.0570,
                now.minusHours(1), now.minusMinutes(45), now.minusMinutes(30), null,
                "https://upload.wikimedia.org/wikipedia/commons/4/4a/Bogota%2C_viewed_from_Monserrate_%285620507403%29.jpg");

        seedService(luis, carlos, "Museo del Oro, Bogotá", 2, ServiceStatus.ACCEPTED, 4.5980, -74.0695,
                now.minusHours(3), now.minusHours(2), null, null,
                "https://upload.wikimedia.org/wikipedia/commons/b/bc/Exposici%C3%B3n_La_Sacerdotisa_de_Chornancap%2C_Per%C3%BA%2C_en_el_Museo_del_Oro_del_Banco_de_la_Rep%C3%BAblica_de_Bogot%C3%A1_-_50833016151.jpg");

        seedService(john, laura, "Plaza de Bolívar, Bogotá", 2, ServiceStatus.WAITING_FOR_START, 4.5983, -74.0750,
                now.minusHours(4), now.minusHours(3), null, null,
                "https://upload.wikimedia.org/wikipedia/commons/e/e5/Plaza_de_Bol%C3%ADvar_en_Bogot%C3%A1_%28Colombia%29.jpg");
        log.info("Services seeded.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedService(Client client, Partner partner,
            String locationDescription, int agreedHours,
            ServiceStatus status,
            double latitude, double longitude,
            LocalDateTime requestedAt, LocalDateTime acceptedAt,
            LocalDateTime startedAt, LocalDateTime endedAt,
            String locationReferenceImageUrl) {
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
            service.setLocationReferenceImageUrl(locationReferenceImageUrl);
            serviceRepository.save(service);
            log.info("Service seeded: {} → {} [{}]", client.getId(), partner.getId(), status);
        } catch (Exception e) {
            log.warn("Could not seed service: {}", e.getMessage());
        }
    }
}