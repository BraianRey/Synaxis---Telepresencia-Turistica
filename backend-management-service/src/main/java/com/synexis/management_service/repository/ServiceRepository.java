package com.synexis.management_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServiceStatus;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findByClient_Id(Long clientId);

    List<ServiceEntity> findByPartner_Id(Long partnerId);

    Optional<ServiceEntity> findById(Long serviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ServiceEntity s WHERE s.idService = :id")
    Optional<ServiceEntity> findByIdForUpdate(@Param("id") Long id);

    List<ServiceEntity> findByArea_IdAndStatus(Long areaId, ServiceStatus status);

    boolean existsByPartner_IdAndStatusIn(Long partnerId, Collection<ServiceStatus> statuses);

    boolean existsByClient_IdAndStatusIn(Long clientId, Collection<ServiceStatus> statuses);

}
