package com.synexis.management_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapped to the {@code users} table. Represents an account that can sign in to the system.
 *
 * <p>How it works: Hibernate creates or updates the table when the app starts ({@code ddl-auto}). The password is never
 * stored in plain text — only a BCrypt hash in {@link #passwordHash}. Email is unique and normalized to lowercase
 * before save in {@link com.synexis.management_service.service.AuthService AuthService}.
 *
 * <p>{@link #profilePicturePath} stores a relative path or key pointing to a file under the {@code picProfile/}
 * directory (e.g. {@code picProfile/uuid.jpg}); the app does not store binary image data in the database.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt hash of the password; column {@code password_hash} in PostgreSQL. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    /**
     * Location of the profile image on disk or logical path (e.g. {@code picProfile/user-1.png}). Nullable until the
     * user uploads or sets a picture.
     */
    @Column(name = "profile_picture_path", length = 512)
    private String profilePicturePath;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }
}
