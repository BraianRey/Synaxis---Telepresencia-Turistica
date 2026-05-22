package com.synexis.management_service.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServicePayment;
import com.synexis.management_service.payment.PaymentPricing;
import com.synexis.management_service.repository.ServicePaymentRepository;
import com.synexis.management_service.service.PaymentService;

/**
 * Placeholder implementation of {@link PaymentService}.
 *
 * <p>
 * Billing uses {@link PaymentPricing}: minimum 30 minutes at 5 USD, then tiered per minute.
 */
@Service
public class NoopPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(NoopPaymentService.class);

    private final ServicePaymentRepository servicePaymentRepository;

    public NoopPaymentService(ServicePaymentRepository servicePaymentRepository) {
        this.servicePaymentRepository = servicePaymentRepository;
    }

    @Override
    public void cancelPreAuthorization(Long serviceId) {
        log.info("NOOP PaymentService: cancel pre-authorization for service {}", serviceId);
    }

    public ServicePayment calculateAndPersist(ServiceEntity service) {
        if (service.getStartedAt() == null || service.getEndedAt() == null) {
            throw new IllegalStateException("Cannot calculate payment: missing timestamps");
        }

        long minutes = ChronoUnit.MINUTES.between(service.getStartedAt(), service.getEndedAt());
        int actualDurationMin = (int) Math.max(1, minutes);
        int billedMinutes = PaymentPricing.billedMinutes(actualDurationMin);

        ServicePayment payment = new ServicePayment();
        payment.setService(service);
        payment.setActualDurationMin(actualDurationMin);
        payment.setBilledMinutes(billedMinutes);
        payment.setRatePerMinute(PaymentPricing.effectiveRatePerMinute(billedMinutes));
        payment.setTotalAmount(PaymentPricing.calculateTotalAmount(billedMinutes));
        payment.setCalculatedAt(LocalDateTime.now());
        payment.setConfirmed(false);

        return servicePaymentRepository.save(payment);
    }
}
