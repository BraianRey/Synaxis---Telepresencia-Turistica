package com.synexis.management_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores idempotency keys for {@code POST /api/services/create} so repeated
 * requests with the same {@code X-Idempotency-Key} return the same service.
 */
@Entity
@Table(name = "service_idempotency_keys", uniqueConstraints = @UniqueConstraint(columnNames = { "client_id",
        "idempotency_key" }))
@Getter
@Setter
@NoArgsConstructor
public class ServiceIdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;
}
