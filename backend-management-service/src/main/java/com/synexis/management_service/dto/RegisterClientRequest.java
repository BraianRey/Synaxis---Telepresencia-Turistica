package com.synexis.management_service.dto;

import com.synexis.management_service.models.UserLanguage;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for {@code POST /register/client}. Aligns with logical {@code User} + {@code Client} in SQLBD.sql. */
public record RegisterClientRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String name,
        @NotNull @AssertTrue Boolean termsAccepted,
        UserLanguage language,
        @Size(max = 255) String picDirectory) {}
