package com.synexis.management_service.repository;

import com.synexis.management_service.models.Partner;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the {@code partners} table — one repository per entity/table. */
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Partner> findByEmailIgnoreCase(String email);
}
