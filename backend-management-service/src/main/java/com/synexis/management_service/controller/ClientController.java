package com.synexis.management_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.response.RegisterClientResponse;
import com.synexis.management_service.service.ClientService;

import jakarta.validation.Valid;

/** REST endpoints for client registration */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterClientResponse registerClient(@Valid @RequestBody RegisterClientRequest request) {
        return clientService.registerClient(request);
    }

}
