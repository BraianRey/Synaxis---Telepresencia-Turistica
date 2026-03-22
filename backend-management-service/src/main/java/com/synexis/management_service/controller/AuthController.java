package com.synexis.management_service.controller;

import com.synexis.management_service.dto.RegisterRequest;
import com.synexis.management_service.dto.RegisterResponse;
import com.synexis.management_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints. Exposes {@code POST /register} for self-service signup.
 *
 * <p>How it works: Accepts JSON {@link RegisterRequest}, validates it with Bean Validation, delegates to
 * {@link AuthService}, returns {@link RegisterResponse} with HTTP {@code 201}. Invalid payloads produce {@code 400}
 * via {@link com.synexis.management_service.exception.RestExceptionHandler RestExceptionHandler}.
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Creates a new user account; public route (see {@link com.synexis.management_service.config.SecurityConfig}). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}
