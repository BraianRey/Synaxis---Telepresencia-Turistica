package com.synexis.management_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServiceStatus;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findByClient_Id(Long clientId);

    List<ServiceEntity> findByPartner_Id(Long partnerId);

    // Only services in "requested" or "accepted" status are considered active
    List<ServiceEntity> findByStatus(ServiceStatus status);

    Optional<ServiceEntity> findById(Long serviceId);

    boolean existsByClient_IdAndStatusIn(Long authenticatedClientId, Set<ServiceStatus> activeServiceStatuses);

    boolean existsByPartner_IdAndStatusIn(Long partnerId, Set<ServiceStatus> activeServiceStatuses);

}
