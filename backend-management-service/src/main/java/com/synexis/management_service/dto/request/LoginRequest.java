package com.synexis.management_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for login endpoints. */
public record LoginRequest(
                @NotBlank @Email @Size(max = 255) String email,
                @NotBlank @Size(min = 8, max = 100) String password) {
}
