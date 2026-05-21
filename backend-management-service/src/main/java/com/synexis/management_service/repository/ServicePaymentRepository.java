package com.synexis.management_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synexis.management_service.entity.ServicePayment;

@Repository
public interface ServicePaymentRepository extends JpaRepository<ServicePayment, Long> {

    Optional<ServicePayment> findByService_IdService(Long serviceId);

}