package com.synexis.management_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponse {

    private Long id;
    private Long serviceId;
    private Long clientId;
    private Long partnerId;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;

}