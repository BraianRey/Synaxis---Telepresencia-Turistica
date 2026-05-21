package com.synexis.management_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(value = 1, message = "Minimum score is 1")
    @Max(value = 5, message = "Maximum score is 5")
    @Column(nullable = false)
    private Integer score;

    @NotBlank(message = "Comment is required")
    @Size(min = 10, max = 500, message = "Comment must be between 10 and 500 characters")
    @Column(nullable = false, length = 500)
    private String comment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    /**
     * OneToOne relationship: a service may have at most one rating.
     * @JoinColumn is used on this side so Rating owns the FK.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, unique = true)
    private ServiceEntity service;
}