package com.synexis.streaming_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Peer {
    private String peerId;
    private String sessionId;
    private PeerRole role;
}
