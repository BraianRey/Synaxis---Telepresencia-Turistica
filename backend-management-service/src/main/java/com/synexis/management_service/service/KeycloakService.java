package com.synexis.management_service.service;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.Optional;

public interface KeycloakService {

    String resolveUserForRole(String email, String password, String name, String roleName);

    Optional<UserRepresentation> findUserByEmail(String email);

    String getClientUuid();

    String createKeycloakUser(String email, String name);

    void setPassword(String userId, String password);

    boolean userHasRole(String userId, String clientUuid, String roleName);

    void assignRoleIfMissing(String userId, String clientUuid, String roleName);

}