package com.synexis.management_service.service;

import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;

public interface PartnerService {

    public RegisterPartnerResponse registerPartner(RegisterPartnerRequest request);

}
