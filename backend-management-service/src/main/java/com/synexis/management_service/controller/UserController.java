package com.synexis.management_service.controller;

import com.synexis.management_service.dto.RegisterClientRequest;
import com.synexis.management_service.dto.RegisterClientResponse;
import com.synexis.management_service.dto.RegisterPartnerRequest;
import com.synexis.management_service.dto.RegisterPartnerResponse;
import com.synexis.management_service.service.ClientService;
import com.synexis.management_service.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for user registration: clients and partners use separate services and tables. */
@RestController
public class UserController {

    private final ClientService clientService;
    private final PartnerService partnerService;

    public UserController(ClientService clientService, PartnerService partnerService) {
        this.clientService = clientService;
        this.partnerService = partnerService;
    }

    @PostMapping("/register/client")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterClientResponse registerClient(@Valid @RequestBody RegisterClientRequest request) {
        return clientService.registerClient(request);
    }

    @PostMapping("/register/partner")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterPartnerResponse registerPartner(@Valid @RequestBody RegisterPartnerRequest request) {
        return partnerService.registerPartner(request);
    }
}
