package com.synexis.management_service.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.synexis.management_service.service.PaymentService;

/**
 * Placeholder implementation of {@link PaymentService}.
 *
 * <p>
 * It only logs the intent; the real implementation should live in the payments
 * module and be wired here via Spring configuration.
 */
@Service
public class NoopPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(NoopPaymentService.class);

    @Override
    public void cancelPreAuthorization(Long serviceId) {
        log.info("NOOP PaymentService: cancel pre-authorization for service {}", serviceId);
    }
}

