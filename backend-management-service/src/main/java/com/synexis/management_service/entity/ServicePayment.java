package com.synexis.management_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_payment")
public class ServicePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "service_id", nullable = false, unique = true)
    private ServiceEntity service;

    @Column(name = "actual_duration_min")
    private Integer actualDurationMin;

    @Column(name = "billed_hours", precision = 10, scale = 4)
    private BigDecimal billedHours;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(nullable = false)
    private Boolean confirmed = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public Integer getActualDurationMin() {
        return actualDurationMin;
    }

    public void setActualDurationMin(Integer actualDurationMin) {
        this.actualDurationMin = actualDurationMin;
    }

    public BigDecimal getBilledHours() {
        return billedHours;
    }

    public void setBilledHours(BigDecimal billedHours) {
        this.billedHours = billedHours;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
}