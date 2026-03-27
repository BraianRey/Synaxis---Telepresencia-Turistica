package com.synexis.management_service.service;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.response.RegisterClientResponse;

public interface ClientService {

    public RegisterClientResponse registerClient(RegisterClientRequest request);

}
