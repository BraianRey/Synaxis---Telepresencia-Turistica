package com.synexis.streaming_service;

import com.synexis.streaming_service.business.impl.SignalingServiceImpl;
import com.synexis.streaming_service.business.services.SessionManager;
import com.synexis.streaming_service.model.Peer;
import com.synexis.streaming_service.model.PeerRole;
import com.synexis.streaming_service.model.Room;
import com.synexis.streaming_service.presentation.dto.request.SignalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SignalingServiceTest {

    private SessionManager sessionManager;
    private SignalingServiceImpl signalingService;

    @BeforeEach
    void setUp() {
        sessionManager = Mockito.mock(SessionManager.class);
        signalingService = new SignalingServiceImpl(sessionManager);
    }

    @Test
    void relaySignal_isNoop_forLegacySignalingLogic() {
        Room room = new Room("room-1");
        room.addPeer(new Peer("partner-1", "session-1", PeerRole.PARTNER));
        room.addPeer(new Peer("client-1", "session-2", PeerRole.CLIENT));

        when(sessionManager.getRoom("room-1")).thenReturn(room);

        SignalRequest request = new SignalRequest();
        request.setType("offer");
        request.setRoomId("room-1");
        request.setFromPeerId("partner-1");
        request.setPayload("sdp-content");

        signalingService.relaySignal(request);

        verifyNoInteractions(sessionManager);
    }
}
