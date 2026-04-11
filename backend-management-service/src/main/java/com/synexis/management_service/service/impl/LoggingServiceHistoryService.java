package com.synexis.management_service.service.impl;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.service.ServiceHistoryService;

/**
 * Simple implementation of {@link ServiceHistoryService} that just logs events.
 *
 * <p>
 * This keeps the domain flow complete while allowing a future persistence-based
 * implementation without touching business logic.
 */
@Service
public class LoggingServiceHistoryService implements ServiceHistoryService {

    private static final Logger log = LoggerFactory.getLogger(LoggingServiceHistoryService.class);

    @Override
    public void recordEvent(ServiceEntity service, String actorType, Long actorId, String message, Instant at) {
        log.info("Service history - serviceId={}, actorType={}, actorId={}, at={}, message={}",
                service.getIdService(),
                actorType,
                actorId,
                at,
                message);
    }
}

