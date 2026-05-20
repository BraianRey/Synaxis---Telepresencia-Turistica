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

    @NotBlank(message = "Comment is required")
    @Size(min = 10, max = 500, message = "Comment must be between 10 and 500 characters")
    private String comment;
}