package com.synexis.management_service.service;

import com.synexis.management_service.entity.ServiceEntity;

/**
 * Abstraction for sending notifications to end users (client or partner)
 * about relevant service lifecycle events.
 *
 * <p>
 * The concrete implementation can deliver notifications via push, email or
 * any other channel.
 * </p>
 */
public interface NotificationService {

    /**
     * Notifies the client that a service has been cancelled by the assigned
     * partner.
     *
     * @param service current state of the cancelled service
     */
    void notifyClientServiceCancelledByPartner(ServiceEntity service);

    /**
     * Notifies the client that the service is ready for telepresence.
     *
     * @param service current state of the ready service
     */
    void notifyClientServiceReady(ServiceEntity service);
}
