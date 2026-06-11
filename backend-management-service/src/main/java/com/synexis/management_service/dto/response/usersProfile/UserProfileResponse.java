package com.synexis.management_service.dto.response.usersProfile;

/**
 * Response DTO for the user profile endpoint.
 */
public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String status,
        String language,
        String role,
        Boolean hasProfilePicture,
        Double averageRating,
        Integer ratingCount,
        String availabilityStatus,
        String city,
        java.time.Instant createdAt
) {}