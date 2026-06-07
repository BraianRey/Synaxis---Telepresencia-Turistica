package com.synexis.management_service.service;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.response.RegisterClientResponse;
import com.synexis.management_service.dto.response.usersProfile.ClientPublicProfileResponse;

public interface ClientService {

    RegisterClientResponse registerClient(RegisterClientRequest request);

    ClientPublicProfileResponse getPublicProfile(Long clientId);

}
