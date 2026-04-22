package com.synexis.streaming_service.business.impl;

import com.synexis.streaming_service.business.services.SessionManager;
import com.synexis.streaming_service.data.RoomRepository;
import com.synexis.streaming_service.exception.RoomNotFoundException;
import com.synexis.streaming_service.model.Peer;
import com.synexis.streaming_service.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SessionManagerImpl implements SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManagerImpl.class);

    private final RoomRepository roomRepository;

    public SessionManagerImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public Room createRoom(String roomId) {
        logger.info("Creando nueva sala: {}", roomId);
        Room room = new Room(roomId);
        return roomRepository.save(room);
    }

    @Override
    public Room joinRoom(String roomId, Peer peer) {
        logger.info("Peer {} ({}) uniéndose a la sala {}", peer.getPeerId(), peer.getRole(), roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
        room.addPeer(peer);
        return room;
    }

    @Override
    public void leaveRoom(String roomId, String peerId) {
        logger.info("Peer {} dejando la sala {}", peerId, roomId);
        roomRepository.findById(roomId).ifPresent(room -> {
            room.removePeer(peerId);
            if (room.isEmpty()) {
                logger.info("Sala {} eliminada por estar vacía", roomId);
                roomRepository.deleteById(roomId);
            }
        });
    }

    @Override
    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }
}
