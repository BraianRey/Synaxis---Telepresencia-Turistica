package com.synexis.streaming_service.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomId) {
        super("Room not found: " + roomId);
    }
}
