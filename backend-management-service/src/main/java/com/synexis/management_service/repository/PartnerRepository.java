package com.synexis.management_service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synexis.management_service.entity.Partner;

/**
 * Persistence for the {@code partners} table — one repository per entity/table.
 */

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByKeycloakId(String keycloakId);

    Optional<Partner> findByEmailIgnoreCase(String email);

    Optional<Partner> findByKeycloakId(String keycloakId);
}
