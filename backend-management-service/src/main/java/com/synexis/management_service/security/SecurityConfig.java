package com.synexis.management_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

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
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        Converter<Jwt, AbstractAuthenticationToken> keycloakJwtAuthenticationConverter)
                        throws Exception {
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
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                keycloakJwtAuthenticationConverter)));
                return http.build();
        }

        /**
         * Maps Keycloak client roles ({@code telepresence}: CLIENT, PARTNER) to
         * {@code ROLE_CLIENT} / {@code ROLE_PARTNER} for {@code @PreAuthorize}.
         */
        @Bean
        public Converter<Jwt, AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {
                return new KeycloakJwtAuthenticationConverter();
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
