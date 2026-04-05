package com.synexis.management_service.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

/**
 * Spring Security setup for the API: which URLs are anonymous, CSRF policy, and
 * the password hashing bean used when
 * saving users.
 *
 * <p>
 * How it works: {@link #securityFilterChain(HttpSecurity)} disables CSRF
 * (typical for stateless JSON APIs) and
 * allows unauthenticated access to {@code /ping}, paths under
 * {@code /register/}, the H2 console, and {@code OPTIONS}
 * preflight; everything else requires an authenticated principal.
 * {@link #passwordEncoder()} is used by {@link
 * com.synexis.management_service.service.impl.AuthServiceImpl AuthService} for
 * encoding and verification.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        /**
         * Omite por completo el filtro de seguridad en estas rutas (evita 403 cuando
         * {@code permitAll} no coincide con el
         * {@code RequestMatcher} de MVC en algunos entornos). Los métodos del
         * controlador no influyen en la URL: solo
         * importan las rutas declaradas en {@code @PostMapping} / {@code @GetMapping}.
         */
        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
                return web -> web.ignoring()
                                .requestMatchers(
                                                "/ping",
                                                "/api/auth/**",
                                                "/api/clients/register",
                                                "/api/partners/register");
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.csrf(AbstractHttpConfigurer::disable)
                                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                                .authorizeHttpRequests(
                                                auth -> auth
                                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                                .requestMatchers(
                                                                                "/ping",
                                                                                "/api/auth/**",
                                                                                "/api/clients/register",
                                                                                "/api/partners/register")
                                                                .permitAll()
                                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
                return http.build();
        }

        /**
         * Maps Keycloak client roles from resource_access.telepresence.roles to ROLE_*
         * authorities so @PreAuthorize("hasRole('CLIENT')") works as expected.
         */
        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(new KeycloakClientRolesConverter("telepresence"));
                return converter;
        }

        static class KeycloakClientRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
                private final String clientId;

                KeycloakClientRolesConverter(String clientId) {
                        this.clientId = clientId;
                }

                @Override
                public Collection<GrantedAuthority> convert(Jwt jwt) {
                        Object resourceAccessObj = jwt.getClaims().get("resource_access");
                        if (!(resourceAccessObj instanceof Map<?, ?> resourceAccess)) {
                                return Collections.emptyList();
                        }

                        Object clientObj = resourceAccess.get(clientId);
                        if (!(clientObj instanceof Map<?, ?> clientMap)) {
                                return Collections.emptyList();
                        }

                        Object rolesObj = clientMap.get("roles");
                        if (!(rolesObj instanceof List<?> roles)) {
                                return Collections.emptyList();
                        }

                        return roles.stream()
                                        .filter(String.class::isInstance)
                                        .map(String.class::cast)
                                        .map(String::toUpperCase)
                                        .map(role -> "ROLE_" + role)
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toSet());
                }
        }

        /**
         * BCrypt strength default (10). Used to hash passwords before storing
         * {@code password_hash} in the database.
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
