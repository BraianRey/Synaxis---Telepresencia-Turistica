package com.synexis.management_service.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.service.NotificationService;

/**
 * Simple logging-based implementation of {@link NotificationService}.
 *
 * <p>
 * This keeps the domain logic decoupled from the actual notification channel
 * while allowing future replacement with a real notification adapter.
 * </p>
 */
@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void notifyClientServiceCancelledByPartner(ServiceEntity service) {
        log.info("Notify client {} that service {} was cancelled by partner {}",
                service.getClient() != null ? service.getClient().getId() : null,
                service.getIdService(),
                service.getPartner() != null ? service.getPartner().getId() : null);
    }

    @Override
    public void notifyClientServiceReady(ServiceEntity service) {
        log.info("Notify client {} that service {} is ready for telepresence",
                service.getClient() != null ? service.getClient().getId() : null,
                service.getIdService());
    }
}
