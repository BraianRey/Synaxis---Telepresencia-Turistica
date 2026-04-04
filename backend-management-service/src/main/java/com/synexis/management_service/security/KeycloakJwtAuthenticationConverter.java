package com.synexis.management_service.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Maps Keycloak JWT claims to Spring Security authorities so that
 * {@code @PreAuthorize("hasRole('CLIENT')")} and {@code hasRole('PARTNER')} work.
 *
 * <p>
 * Default {@link JwtGrantedAuthoritiesConverter} only maps {@code scope} to
 * {@code SCOPE_*} authorities. Keycloak exposes application roles under
 * {@code resource_access.&lt;clientId&gt;.roles} (e.g. {@code telepresence} →
 * {@code CLIENT}, {@code PARTNER}).
 * </p>
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLIENT_ID = "telepresence";

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
        authorities.addAll(extractKeycloakRoles(jwt));
        return new JwtAuthenticationToken(jwt, authorities);
    }

    @SuppressWarnings("unchecked")
    private static Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return Collections.emptyList();
        }
        Object clientAccess = resourceAccess.get(CLIENT_ID);
        if (!(clientAccess instanceof Map)) {
            return Collections.emptyList();
        }
        Object rolesObj = ((Map<String, Object>) clientAccess).get("roles");
        if (!(rolesObj instanceof List)) {
            return Collections.emptyList();
        }
        List<String> roles = (List<String>) rolesObj;
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
    }
}
