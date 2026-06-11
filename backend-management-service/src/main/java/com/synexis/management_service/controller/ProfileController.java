package com.synexis.management_service.controller;

import com.synexis.management_service.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.synexis.management_service.dto.ProfilePictureDto;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final UserProfileService userProfileService;
    private final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping(value = "/users/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        try {
            logger.debug("uploadProfilePicture called, filePresent={} size={}", file != null, file == null ? 0L : file.getSize());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String keycloakId = auth == null ? null : auth.getName();
            logger.debug("Authenticated keycloakId={}", keycloakId);
            if (keycloakId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthenticated"));
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file was uploaded."));
            }

            String contentType = file.getContentType();
            if (contentType == null || !Set.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported file type. Allowed: JPEG, PNG, WebP."));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds maximum allowed 5 MB."));
            }

            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (java.io.IOException ioe) {
                return ResponseEntity.internalServerError().body(Map.of("error", ioe.getMessage()));
            }

            userProfileService.saveProfilePicture(keycloakId, bytes, contentType);
            logger.debug("saveProfilePicture delegated to service for keycloakId={}", keycloakId);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/upload/profile-pic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadProfilePicAlias(
            @RequestParam("file") MultipartFile file) {
        return uploadProfilePicture(file);
    }

    @GetMapping(value = "/users/me/profile-picture")
    public ResponseEntity<byte[]> getMyProfilePicture() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String keycloakId = auth == null ? null : auth.getName();
        if (keycloakId == null) {
            return ResponseEntity.status(401).build();
        }

        ProfilePictureDto pic = userProfileService.getProfilePicture(keycloakId);
        if (pic == null || pic.data() == null || pic.data().length == 0) {
            return ResponseEntity.status(404).build();
        }

        String contentType = pic.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : pic.contentType();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(pic.data());
    }
}
