package com.synexis.management_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service")
@Getter
@Setter
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idService;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "clients_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "partners_id", nullable = false)
    private Partner partner;

    @ManyToOne
    @JoinColumn(name = "areas_id", nullable = false)
    private Area area;

    // Campos
    @Column(length = 255)
    private String startLocationDescription;

    private Integer agreedHours;

    @Column(precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

}