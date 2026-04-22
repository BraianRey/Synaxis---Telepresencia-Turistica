package com.synexis.streaming_service.presentation.dto.request;

import com.synexis.streaming_service.model.PeerRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinRoomRequest {
    @NotBlank
    private String roomId;
    @NotBlank
    private String peerId;
    @NotNull
    private PeerRole role;
}
