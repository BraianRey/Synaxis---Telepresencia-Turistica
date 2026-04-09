package com.synexis.streaming_service.business.services;

import com.synexis.streaming_service.model.Peer;
import com.synexis.streaming_service.model.Room;

public interface SessionManager {
    Room createRoom(String roomId);
    Room joinRoom(String roomId, Peer peer);
    void leaveRoom(String roomId, String peerId);
    Room getRoom(String roomId);
}
