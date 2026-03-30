package com.synexis.management_service.config;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.entity.Area;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.repository.AreaRepository;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.ClientService;
import com.synexis.management_service.service.PartnerService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
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
    private final AreaRepository areaRepository;

    public DataInitializer(ClientService clientService,
            PartnerService partnerService,
            ClientRepository clientRepository,
            PartnerRepository partnerRepository,
            AreaRepository areaRepository) {
        this.clientService = clientService;
        this.partnerService = partnerService;
        this.clientRepository = clientRepository;
        this.partnerRepository = partnerRepository;
        this.areaRepository = areaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== DataInitializer: starting seed ===");
        seedAreas();
        seedClients();
        seedPartners();
        log.info("=== DataInitializer: seed complete ===");
    }

    // ------------------------------------------------------------------
    // AREAS — inserted via data.sql, just log count here
    // ------------------------------------------------------------------
    // Reemplaza el método seedAreas() por este:
    private void seedAreas() {
        if (areaRepository.count() > 0) {
            log.info("Areas already seeded, skipping ({} found)", areaRepository.count());
            return;
        }
        areaRepository.saveAll(List.of(
                createArea("Colombia", "Cauca", "Popayán", 2.4448, -76.6147),
                createArea("Colombia", "Valle", "Cali", 3.4516, -76.5320),
                createArea("Colombia", "Antioquia", "Medellín", 6.2442, -75.5812),
                createArea("Colombia", "Cundinamarca", "Bogotá", 4.7110, -74.0721)));
        log.info("Areas seeded: 4");
    }

    private Area createArea(String country, String state,
            String municipality, Double lat, Double lng) {
        Area area = new Area();
        area.setCountry(country);
        area.setState(state);
        area.setMunicipality(municipality);
        area.setCenterLat(lat);
        area.setCenterLng(lng);
        return area;
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
        seedPartner("Carlos Guía", "carlos.seed@gmail.com", "password12", 1, UserLanguage.es);
        seedPartner("Laura Viajes", "laura.seed@gmail.com", "password12", 2, UserLanguage.es);
        seedPartner("Pedro Tours", "pedro.seed@gmail.com", "password12", 3, UserLanguage.es);
        seedPartner("Sofia Explora", "sofia.seed@gmail.com", "password12", 4, UserLanguage.en);
    }

    private void seedPartner(String name, String email, String password,
            Integer areaId, UserLanguage language) {
        if (partnerRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.info("Partner already exists, skipping: {}", email);
            return;
        }
        try {
            partnerService.registerPartner(new RegisterPartnerRequest(
                    email, password, name, areaId, true, language, null));
            log.info("Partner seeded: {}", email);
        } catch (Exception e) {
            log.warn("Could not seed partner {}: {}", email, e.getMessage());
        }
    }
}