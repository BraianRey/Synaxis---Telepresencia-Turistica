package com.synexis.management_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Service provider account. Logical model: {@code Partner} adds
 * {@code Area_idArea} and
 * {@code availability_status} to {@code User} (SQLBD.sql).
 */
@Entity
@Table(name = "partners")
@Getter
@Setter
@NoArgsConstructor
public class Partner extends UserBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "areas_id", nullable = false)
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private PartnerAvailabilityStatus availabilityStatus = PartnerAvailabilityStatus.available;
}