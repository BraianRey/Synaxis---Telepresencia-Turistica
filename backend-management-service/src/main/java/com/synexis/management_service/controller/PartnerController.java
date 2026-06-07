package com.synexis.management_service.controller;

import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;
import com.synexis.management_service.dto.response.usersProfile.ClientPublicProfileResponse;
import com.synexis.management_service.service.ClientService;
import com.synexis.management_service.service.PartnerService;

import jakarta.validation.Valid;
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

/** REST endpoints for partner registration */
@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private final PartnerService partnerService;
    private final ClientService clientService;

    public PartnerController(PartnerService partnerService, ClientService clientService) {
        this.partnerService = partnerService;
        this.clientService = clientService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterPartnerResponse registerPartner(@Valid @RequestBody RegisterPartnerRequest request) {
        return partnerService.registerPartner(request);
    }

    @GetMapping("/clients/{clientId}/profile")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<ClientPublicProfileResponse>getClientProfile(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientService.getPublicProfile(clientId));
    }
}
