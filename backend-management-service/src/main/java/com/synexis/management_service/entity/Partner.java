package com.synexis.management_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Service provider account. Logical model: {@code Partner} adds
 * {@code Area} (embedded location data) and
 * {@code availability_status} to {@code User} (SQLBD.sql).
 */
@Entity
@Table(name = "partners")
@Getter
@Setter
@NoArgsConstructor
public class Partner extends UserBase {

    @Embedded
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private PartnerAvailabilityStatus availabilityStatus = PartnerAvailabilityStatus.available;
}