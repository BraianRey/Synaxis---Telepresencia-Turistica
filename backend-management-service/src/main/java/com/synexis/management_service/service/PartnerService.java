package com.synexis.management_service.service;

import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;
import com.synexis.management_service.dto.response.usersProfile.PartnerPublicProfileResponse;

public interface PartnerService {

    RegisterPartnerResponse registerPartner(RegisterPartnerRequest request);

    PartnerPublicProfileResponse getPublicProfile(Long partnerId);
}
