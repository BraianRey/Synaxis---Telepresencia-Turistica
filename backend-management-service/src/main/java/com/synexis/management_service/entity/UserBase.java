package com.synexis.management_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fields shared with the logical {@code User} table in SQLBD.sql (name, email,
 * password_hash,
 * status, language, created_at, terms_accepted, rol). Embedded in
 * {@link Client}
 * and {@link Partner} physical tables for this service.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class UserBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", nullable = false)
    private String keycloakId;

    /** {@code User.name} — display name (client) or trade/public name (partner). */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private UserLanguage language = UserLanguage.es;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "terms_accepted", nullable = false)
    private Boolean termsAccepted;

    /**
     * Same as logical {@code User.rol}; stored as {@code user_role} to avoid
     * reserved SQL keywords.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole role;

    /** Profile picture stored as binary in DB using PostgreSQL BYTEA. */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "profile_picture", columnDefinition = "bytea")
    private byte[] profilePicture;

    @Column(name = "profile_picture_content_type", length = 100)
    private String profilePictureContentType;
}
