package com.synexis.management_service.service;

import java.time.Instant;

import com.synexis.management_service.entity.ServiceEntity;

/**
 * Abstraction for recording events in the service history/audit log.
 */
public interface ServiceHistoryService {

    /**
     * Records a status change or lifecycle event for a service.
     *
     * @param service   current persisted service entity
     * @param actorType logical actor type (for example, CLIENT, PARTNER, SYSTEM)
     * @param actorId   optional actor identifier (client/partner id)
     * @param message   human readable description
     * @param at        timestamp of the event
     */
    void recordEvent(ServiceEntity service, String actorType, Long actorId, String message, Instant at);
}

