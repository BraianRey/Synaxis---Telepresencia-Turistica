package com.synexis.management_service.repository;

import com.synexis.management_service.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link User}. Provides CRUD via
 * {@link JpaRepository} and custom queries derived
 * from method names.
 *
 * <p>
 * How it works: Spring implements this interface at runtime.
 * {@link #existsByEmailIgnoreCase(String)} and
 * {@link #findByEmailIgnoreCase(String)} translate to SQL with
 * {@code LOWER(email)} semantics for case-insensitive
 * matching consistent with how emails are normalized in the service layer.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Returns whether any user already has this email (case-insensitive). */
    boolean existsByEmailIgnoreCase(String email);

    /** Loads a user by email when present (case-insensitive). */
    Optional<User> findByEmailIgnoreCase(String email);
}
