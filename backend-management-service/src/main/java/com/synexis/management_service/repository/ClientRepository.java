package com.synexis.management_service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synexis.management_service.entity.Client;

/**
 * Persistence for the {@code clients} table — one repository per entity/table.
 */

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByKeycloakId(String keycloakId);

    Optional<Client> findByEmailIgnoreCase(String email);

    Optional<Client> findByKeycloakId(String keycloakId);
}
