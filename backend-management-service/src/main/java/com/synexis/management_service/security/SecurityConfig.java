package com.synexis.management_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .requestMatchers("/ping", "/register/**", "/h2-console", "/h2-console/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**")
                                .permitAll()
                                .requestMatchers(
                                        "/ping",
                                        "/register/**",
                                        "/h2-console",
                                        "/h2-console/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated());
        return http.build();
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
