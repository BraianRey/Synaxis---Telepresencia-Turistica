package com.synexis.management_service.service;

import com.synexis.management_service.dto.response.usersProfile.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getMyProfile(String keycloakId);
    

}
