package com.synexis.streaming_service.model;

import lombok.Data;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {
    private final String roomId;
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    public void addPeer(Peer peer) {
        peers.put(peer.getPeerId(), peer);
    }

    public void removePeer(String peerId) {
        peers.remove(peerId);
    }

    public boolean isEmpty() {
        return peers.isEmpty();
    }
}
