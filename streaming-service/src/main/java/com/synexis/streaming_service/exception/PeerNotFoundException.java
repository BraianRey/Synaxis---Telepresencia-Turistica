package com.synexis.streaming_service.exception;

public class PeerNotFoundException extends RuntimeException {
    public PeerNotFoundException(String peerId) {
        super("Peer not found: " + peerId);
    }
}
