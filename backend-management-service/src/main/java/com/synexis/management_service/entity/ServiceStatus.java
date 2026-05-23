package com.synexis.management_service.entity;

public enum ServiceStatus {
    REQUESTED,
    ACCEPTED,
    WAITING_FOR_START,
    READY,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    EXPIRED
}