package com.synexis.management_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.synexis.management_service.entity.Area;

/**
 * Repository for Area entity.
 */
public interface AreaRepository extends JpaRepository<Area, Long> {

}