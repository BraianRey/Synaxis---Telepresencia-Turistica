package com.synexis.management_service.controller;

import com.synexis.management_service.dto.request.LocationUpdateRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for location/GPS tracking.
 * Receives location updates from mobile partners.
 */
@RestController
@RequestMapping("/location")
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    /**
     * Receives GPS coordinates from the mobile partner application.
     *
     * @param locationRequest DTO containing latitude and longitude
     * @return ResponseEntity with success status
     */
    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> updateLocation(@Valid @RequestBody LocationUpdateRequest locationRequest) {

        // Print location to console
        System.out.println("========================================");
        System.out.println("Received location update from partner:");
        System.out.println(
                "Coordinates: (" + locationRequest.getLatitude() + ", " + locationRequest.getLongitude() + ")");
        System.out.println("Timestamp: " + System.currentTimeMillis());
        System.out.println("========================================");

        // Also log using SLF4J for production logging
        logger.info("Location update received from partner - Latitude: {}, Longitude: {}",
                locationRequest.getLatitude(),
                locationRequest.getLongitude());

        return ResponseEntity.ok().build();
    }
}
