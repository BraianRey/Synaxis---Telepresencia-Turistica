package com.synexis.management_service.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.synexis.management_service.dto.request.RatingRequest;
import com.synexis.management_service.dto.response.RatingResponse;
import com.synexis.management_service.service.RatingService;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RatingResponse> createRating(
            @Valid @RequestBody RatingRequest request) {
        RatingResponse response = ratingService.createRating(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PARTNER')")
    public ResponseEntity<RatingResponse> getRatingById(@PathVariable Long id) {
        return ResponseEntity.ok(ratingService.getRatingById(id));
    }

    @GetMapping("/partner/{partnerId}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<RatingResponse>> getRatingsByPartner(
            @PathVariable Long partnerId) {
        return ResponseEntity.ok(ratingService.getRatingsByPartner(partnerId));
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PARTNER')")
    public ResponseEntity<RatingResponse> getRatingByService(
            @PathVariable Long serviceId) {
        return ResponseEntity.ok(ratingService.getRatingByService(serviceId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RatingResponse> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ratingService.updateRating(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
        ratingService.deleteRating(id);
        return ResponseEntity.noContent().build();
    }
}