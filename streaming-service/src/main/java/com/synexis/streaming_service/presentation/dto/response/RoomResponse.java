package com.synexis.streaming_service.presentation.dto.response;

import com.synexis.streaming_service.model.PeerRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class RoomResponse {
    private String roomId;
    private List<PeerInfo> peers;

    @Data
    @AllArgsConstructor
    public static class PeerInfo {
        private String peerId;
        private PeerRole role;
    }
}
