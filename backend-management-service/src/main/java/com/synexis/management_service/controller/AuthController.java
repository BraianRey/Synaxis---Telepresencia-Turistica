package com.synexis.management_service.controller;

import com.synexis.management_service.dto.request.LoginRequest;
import com.synexis.management_service.dto.response.LoginResponse;
import com.synexis.management_service.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints for mobile apps. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/client/login")
    public ResponseEntity<LoginResponse> loginClient(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.loginClient(request.email(), request.password()));
    }

    @PostMapping("/partner/login")
    public ResponseEntity<LoginResponse> loginPartner(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.loginPartner(request.email(), request.password()));
    }
}

