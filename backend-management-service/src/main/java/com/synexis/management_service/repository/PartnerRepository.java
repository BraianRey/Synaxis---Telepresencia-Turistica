package com.synexis.management_service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.synexis.management_service.entity.Partner;

/**
 * Persistence for the {@code partners} table — one repository per entity/table.
 */
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Partner> findByEmailIgnoreCase(String email);
}
