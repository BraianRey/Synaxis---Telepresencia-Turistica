package com.synexis.management_service.service;

import com.synexis.management_service.dto.ProfilePictureDto;
import com.synexis.management_service.dto.response.usersProfile.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getMyProfile(String keycloakId);

    void saveProfilePicture(String keycloakId, byte[] content, String contentType);

    ProfilePictureDto getProfilePicture(String keycloakId);

    ProfilePictureDto getProfilePictureById(Long userId);
}
