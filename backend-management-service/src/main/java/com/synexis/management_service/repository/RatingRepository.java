package com.synexis.management_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Rating;
import com.synexis.management_service.entity.ServiceEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByService(ServiceEntity service);

    Optional<Rating> findByService(ServiceEntity service);

    List<Rating> findByClient(Client client);

    List<Rating> findByPartnerId(Long partnerId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.partner.id = :partnerId")
    Optional<Double> findAverageScoreByPartnerId(@Param("partnerId") Long partnerId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.partner.id = :partnerId")
    Long countByPartnerId(@Param("partnerId") Long partnerId);
}