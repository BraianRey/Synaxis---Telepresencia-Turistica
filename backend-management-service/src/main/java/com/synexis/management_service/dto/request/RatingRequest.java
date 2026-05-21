package com.synexis.management_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Minimum score is 1")
    @Max(value = 5, message = "Maximum score is 5")
    private Integer score;

    @Size(max = 200, message = "Comment must not exceed 200 characters")
    private String comment;
}