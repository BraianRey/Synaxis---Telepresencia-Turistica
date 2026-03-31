package com.synexis.management_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synexis.management_service.entity.Service;

public interface ServiceRepository extends JpaRepository<Service, Long> {

}
