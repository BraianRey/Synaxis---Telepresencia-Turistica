package com.synexis.streaming_service.presentation.controllers;

import com.synexis.streaming_service.business.services.SessionManager;
import com.synexis.streaming_service.model.Room;
import com.synexis.streaming_service.presentation.dto.request.CreateRoomRequest;
import com.synexis.streaming_service.presentation.dto.response.RoomResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final SessionManager sessionManager;

    public RoomController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("RoomController is healthy");
    }
    

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        String roomId = UUID.randomUUID().toString();
        Room room = sessionManager.createRoom(roomId);
        return ResponseEntity.ok(toResponse(room));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomId) {
        Room room = sessionManager.getRoom(roomId);
        return ResponseEntity.ok(toResponse(room));
    }

    private RoomResponse toResponse(Room room) {
        List<RoomResponse.PeerInfo> peers = room.getPeers().values().stream()
                .map(peer -> new RoomResponse.PeerInfo(peer.getPeerId(), peer.getRole()))
                .collect(Collectors.toList());
        return new RoomResponse(room.getRoomId(), peers);
    }
}
