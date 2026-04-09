package com.synexis.streaming_service.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignalRequest {
    @NotBlank
    private String type;      // "offer", "answer", "ice-candidate"
    @NotBlank
    private String roomId;
    @NotBlank
    private String fromPeerId;
    @NotBlank
    private String targetPeerId;
    private String payload;   // SDP string o ICE candidate JSON
}
