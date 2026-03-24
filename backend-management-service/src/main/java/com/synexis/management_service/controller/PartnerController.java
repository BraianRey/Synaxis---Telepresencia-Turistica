package com.synexis.management_service.controller;

import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;
import com.synexis.management_service.service.PartnerService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for partner registration */
@RestController
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping("/register/partner")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterPartnerResponse registerPartner(@Valid @RequestBody RegisterPartnerRequest request) {
        return partnerService.registerPartner(request);
    }
}
