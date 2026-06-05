package com.synexis.management_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServiceStatus;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    @EntityGraph(attributePaths = "payment")
    List<ServiceEntity> findByClient_Id(Long clientId);

    @EntityGraph(attributePaths = "payment")
    List<ServiceEntity> findByPartner_Id(Long partnerId);

    // Only services in "requested" or "accepted" status are considered active
    List<ServiceEntity> findByStatus(ServiceStatus status);

    Optional<ServiceEntity> findById(Long serviceId);

    List<ServiceEntity> findByClient_IdAndStatusIn(Long clientId, List<ServiceStatus> statuses);

    boolean existsByClient_IdAndStatusIn(Long authenticatedClientId, Set<ServiceStatus> activeServiceStatuses);

    boolean existsByPartner_IdAndStatusIn(Long partnerId, Set<ServiceStatus> activeServiceStatuses);

    @Query("SELECT s FROM ServiceEntity s WHERE s.partner.email = ?1 AND s.status = ?2")
    Optional<ServiceEntity> findByPartnerEmailAndStatus(String email, ServiceStatus status);

    @Query("SELECT s FROM ServiceEntity s WHERE s.partner.email = ?1 AND s.status IN ?2")
    List<ServiceEntity> findByPartnerEmailAndStatusIn(String email, Set<ServiceStatus> statuses);

}
