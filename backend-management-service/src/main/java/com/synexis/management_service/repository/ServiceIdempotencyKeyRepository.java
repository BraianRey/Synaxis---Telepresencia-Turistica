package com.synexis.management_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synexis.management_service.entity.ServiceIdempotencyKey;

public interface ServiceIdempotencyKeyRepository extends JpaRepository<ServiceIdempotencyKey, Long> {

    Optional<ServiceIdempotencyKey> findByClientIdAndIdempotencyKey(Long clientId, String idempotencyKey);
}
