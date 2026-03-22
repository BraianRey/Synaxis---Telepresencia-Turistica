package com.synexis.management_service.repository;

import com.synexis.management_service.models.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the {@code clients} table — one repository per entity/table. */
public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Client> findByEmailIgnoreCase(String email);
}
