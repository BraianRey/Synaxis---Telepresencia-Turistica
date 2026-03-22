package com.synexis.management_service.models;

/**
 * Application role for a registered user. Persisted as a string in the database ({@code VARCHAR}) via JPA
 * {@link jakarta.persistence.EnumType#STRING EnumType.STRING}.
 *
 * <p>{@link #CLIENT} — end user who requests telepresence services. {@link #SOCIO} — provider who streams
 * and executes on-site actions.
 */
public enum Role {
    CLIENT,
    SOCIO
}
