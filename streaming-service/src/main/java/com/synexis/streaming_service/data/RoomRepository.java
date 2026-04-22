package com.synexis.streaming_service.data;

import com.synexis.streaming_service.model.Room;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RoomRepository {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room save(Room room) {
        rooms.put(room.getRoomId(), room);
        return room;
    }

    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public void deleteById(String roomId) {
        rooms.remove(roomId);
    }

    public boolean existsById(String roomId) {
        return rooms.containsKey(roomId);
    }
}
