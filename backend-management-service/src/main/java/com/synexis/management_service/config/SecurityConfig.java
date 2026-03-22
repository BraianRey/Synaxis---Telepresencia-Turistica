package com.synexis.management_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security setup for the API: which URLs are anonymous, CSRF policy, and the password hashing bean used when
 * saving users.
 *
 * <p>How it works: {@link #securityFilterChain(HttpSecurity)} disables CSRF (typical for stateless JSON APIs) and
 * allows unauthenticated access only to {@code /ping} and {@code POST /register}; everything else requires an
 * authenticated principal. {@link #passwordEncoder()} supplies BCrypt for {@link org.springframework.security.crypto.password.PasswordEncoder
 * PasswordEncoder} injection in {@link com.synexis.management_service.service.AuthService AuthService}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/ping", "/register").permitAll().anyRequest().authenticated());
        return http.build();
    }

    /** BCrypt strength default (10). Used to hash passwords before storing {@code password_hash} in the database. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
