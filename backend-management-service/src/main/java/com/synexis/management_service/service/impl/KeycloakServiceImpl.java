package com.synexis.management_service.service.impl;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.exception.KeycloakUserCreationException;
import com.synexis.management_service.service.KeycloakService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class KeycloakServiceImpl implements KeycloakService {

    private final Keycloak keycloak;
    private final String realm;
    private final String clientId;

    // Lazy-cached: the "telepresence" client UUID does not change at runtime.
    // AtomicReference avoids explicit synchronization while preserving thread safety.
    private final AtomicReference<String> cachedClientUuid = new AtomicReference<>();

    public KeycloakServiceImpl(
            Keycloak keycloak,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id:telepresence}") String clientId) {
        this.keycloak = keycloak;
        this.realm    = realm;
        this.clientId = clientId;
    }

    // =========================================================================
    // Main public API
    // =========================================================================

    /**
     * Single entry point for registering a user with a specific role.
     *
     * Behavior:
     * - If the user does NOT exist in Keycloak → creates it, sets the password, and assigns the role.
     * - If the user already exists and does NOT have the role → assigns the role only (password remains intact).
     * - If the user already exists and already has the role → throws EmailAlreadyExistsException.
     *
     * This design ensures the same email can have both CLIENT and PARTNER
     * simultaneously without duplicating users in Keycloak.
     *
     * @param email    normalized email (trim + lowercase already applied by the caller)
     * @param password plain-text password (only used if the user is new)
     * @param name     user name
     * @param roleName name of the client-level role to assign ("CLIENT" or "PARTNER")
     * @return userId UUID of the user in Keycloak
     */
    @Override
    public String resolveUserForRole(String email, String password, String name, String roleName) {
        String clientUuid = getClientUuid();
        Optional<UserRepresentation> existing = findUserByEmail(email);

        String userId;
        if (existing.isEmpty()) {
            userId = createKeycloakUser(email, name);
            setPassword(userId, password);
        } else {
            userId = existing.get().getId();
            if (userHasRole(userId, clientUuid, roleName)) {
                throw new EmailAlreadyExistsException(email);
            }
            // Preexisting roles (e.g. PARTNER) are preserved intact.
        }

        assignRoleIfMissing(userId, clientUuid, roleName);
        return userId;
    }

    // =========================================================================
    // Support methods — public to ease unit testing
    // =========================================================================

    /**
     * Finds a user in Keycloak by email with exact match.
     */
    @Override
    public Optional<UserRepresentation> findUserByEmail(String email) {
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .searchByEmail(email, true); // true = exact match
        return users.stream().findFirst();
    }

    /**
     * Creates a new user in Keycloak and returns its UUID.
     */
    @Override
    public String createKeycloakUser(String email, String name) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setEnabled(true);
        user.setFirstName(name);
        user.setLastName(name);
        user.setEmailVerified(true);

        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() == 409) {
            throw new EmailAlreadyExistsException(email);
        }
        if (response.getStatus() != 201) {
            throw new KeycloakUserCreationException(response.getStatus());
        }

        return extractUserId(response);
    }

    /**
     * Sets the password for a user. Only called for newly created users.
     */
    @Override
    public void setPassword(String userId, String rawPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(rawPassword);
        credential.setTemporary(false);
        keycloak.realm(realm).users().get(userId).resetPassword(credential);
    }

    /**
     * Returns the configured client's internal UUID (e.g. "telepresence").
     * The result is cached in memory after the first successful call.
     */
    @Override
    public String getClientUuid() {
        String uuid = cachedClientUuid.get();
        if (uuid != null) {
            return uuid;
        }

        List<ClientRepresentation> clients = keycloak.realm(realm)
                .clients()
                .findByClientId(clientId);

        if (clients == null || clients.isEmpty()) {
            throw new KeycloakUserCreationException(
                    "Keycloak client '" + clientId + "' not found in realm '" + realm + "'.");
        }

        String resolved = clients.get(0).getId();
        // compareAndSet: if two threads reach this simultaneously, only one wins.
        // The loser simply discards its value — both results are identical.
        cachedClientUuid.compareAndSet(null, resolved);
        return cachedClientUuid.get();
    }

    /**
     * Checks whether a user has a specific client-level role.
     */
    @Override
    public boolean userHasRole(String userId, String clientUuid, String roleName) {
        return keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(clientUuid)
                .listAll()
                .stream()
                .anyMatch(role -> roleName.equals(role.getName()));
    }

    /**
     * Assigns a client-level role to the user if they do not already have it.
     * It is idempotent: if the role already exists, it does nothing.
     */
    @Override
    public void assignRoleIfMissing(String userId, String clientUuid, String roleName) {
        if (userHasRole(userId, clientUuid, roleName)) {
            return;
        }
        try {
            RoleRepresentation role = keycloak.realm(realm)
                    .clients()
                    .get(clientUuid)
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .roles()
                    .clientLevel(clientUuid)
                    .add(List.of(role));

        } catch (Exception ex) {
            throw new KeycloakUserCreationException(
                    "Failed to assign role '" + roleName + "' to userId '" + userId + "': " + ex.getMessage());
        }
    }

    // =========================================================================
    // Internal private methods
    // =========================================================================

    private String extractUserId(Response response) {
        return response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
    }
}