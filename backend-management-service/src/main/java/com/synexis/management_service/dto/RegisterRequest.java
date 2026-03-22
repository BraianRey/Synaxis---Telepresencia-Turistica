package com.synexis.management_service.dto;

import com.synexis.management_service.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for {@code POST /register}. Holds the raw password only in transit; it is never persisted.
 *
 * <p>How it works: Jackson deserializes JSON into this record. {@link jakarta.validation.Valid Valid} on the
 * controller triggers Bean Validation — email format, non-blank fields, password length 8–100, and required {@link Role}.
 * Optional {@code profilePicturePath} holds the stored path under {@code picProfile/} (no file bytes in this request).
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull Role role,
        @Size(max = 512) String profilePicturePath) {}
