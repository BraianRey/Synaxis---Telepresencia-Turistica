package com.synexis.streaming_service;

import com.synexis.streaming_service.business.impl.SessionManagerImpl;
import com.synexis.streaming_service.data.RoomRepository;
import com.synexis.streaming_service.exception.RoomNotFoundException;
import com.synexis.streaming_service.model.Peer;
import com.synexis.streaming_service.model.PeerRole;
import com.synexis.streaming_service.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManagerImpl sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManagerImpl(new RoomRepository());
    }

    @Test
    void createRoom_returnsRoomWithCorrectId() {
        Room room = sessionManager.createRoom("room-1");
        assertEquals("room-1", room.getRoomId());
    }

    @Test
    void joinRoom_addsPeerToRoom() {
        sessionManager.createRoom("room-1");
        Peer peer = new Peer("partner-1", "session-1", PeerRole.PARTNER);
        Room room = sessionManager.joinRoom("room-1", peer);
        assertTrue(room.getPeers().containsKey("partner-1"));
    }

    @Test
    void leaveRoom_removesEmptyRoom() {
        sessionManager.createRoom("room-1");
        Peer peer = new Peer("partner-1", "session-1", PeerRole.PARTNER);
        sessionManager.joinRoom("room-1", peer);
        sessionManager.leaveRoom("room-1", "partner-1");
        assertThrows(RoomNotFoundException.class,
            () -> sessionManager.getRoom("room-1"));
    }

    @Test
    void getRoom_throwsIfNotFound() {
        assertThrows(RoomNotFoundException.class,
            () -> sessionManager.getRoom("no-existe"));
    }
}
