package com.synexis.management_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synexis.management_service.entity.ServiceEntity;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findByClient_Id(Long clientId);

    List<ServiceEntity> findByPartner_Id(Long partnerId);

    Optional<ServiceEntity> findById(Long serviceId);

}
