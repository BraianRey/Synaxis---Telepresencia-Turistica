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

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findByClient_Id(Long clientId);

    List<ServiceEntity> findByPartner_Id(Long partnerId);

    Optional<ServiceEntity> findById(Long serviceId);

}
