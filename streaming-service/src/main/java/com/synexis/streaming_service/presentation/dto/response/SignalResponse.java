package com.synexis.streaming_service.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignalResponse {
    private String type;
    private String roomId;
    private String fromPeerId;
    private String payload;
}
