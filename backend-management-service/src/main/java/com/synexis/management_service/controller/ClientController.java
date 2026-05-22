package com.synexis.management_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.response.RegisterClientResponse;
import com.synexis.management_service.dto.response.usersProfile.PartnerPublicProfileResponse;
import com.synexis.management_service.service.ClientService;
import com.synexis.management_service.service.PartnerService;

import jakarta.validation.Valid;

/** REST endpoints for client registration */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final PartnerService partnerService;

    public ClientController(ClientService clientService, PartnerService partnerService) {
        this.clientService = clientService;
        this.partnerService = partnerService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterClientResponse registerClient(@Valid @RequestBody RegisterClientRequest request) {
        return clientService.registerClient(request);
    }

    @GetMapping("/partners/{partnerId}/profile")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PartnerPublicProfileResponse>getPartnerProfile(@PathVariable Long partnerId) {
        return ResponseEntity.ok(partnerService.getPublicProfile(partnerId));
    }

}
