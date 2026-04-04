package com.synexis.management_service.service;

/**
 * Abstraction for payment operations related to a service lifecycle.
 *
 * <p>
 * The concrete implementation should integrate with the payments module or
 * external gateway.
 */
public interface PaymentService {

    /**
     * Cancels any pre-authorization associated with the given service.
     *
     * @param serviceId identifier of the service whose pre-authorization should
     *                  be cancelled
     */
    void cancelPreAuthorization(Long serviceId);
}

