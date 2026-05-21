package com.synexis.management_service.service;

import com.synexis.management_service.dto.request.RatingRequest;
import com.synexis.management_service.dto.response.RatingResponse;

import java.util.List;

public interface RatingService {

    RatingResponse createRating(RatingRequest request);

    RatingResponse getRatingById(Long id);

    List<RatingResponse> getRatingsByPartner(Long partnerId);

    RatingResponse getRatingByService(Long serviceId);

    RatingResponse updateRating(Long id, RatingRequest request);

    void deleteRating(Long id);
}
