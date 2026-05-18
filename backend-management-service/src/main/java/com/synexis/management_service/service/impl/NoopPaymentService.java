package com.synexis.management_service.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.synexis.management_service.entity.ServiceEntity;
import com.synexis.management_service.entity.ServicePayment;
import com.synexis.management_service.repository.ServicePaymentRepository;
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

        BigDecimal billedHours = BigDecimal.valueOf(actualDurationMin)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        BigDecimal hourlyRate = BigDecimal.valueOf(10.00);
        // TODO: replace hardcoded rate once Partner/Area hourly rate is available.
        if (service.getPartner() != null) {
            log.debug("Partner present for payment calculation, using fallback hourly rate");
        }

        BigDecimal totalAmount = billedHours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);

        ServicePayment payment = new ServicePayment();
        payment.setService(service);
        payment.setActualDurationMin(actualDurationMin);
        payment.setBilledHours(billedHours);
        payment.setHourlyRate(hourlyRate);
        payment.setTotalAmount(totalAmount);
        payment.setCalculatedAt(LocalDateTime.now());
        payment.setConfirmed(false);

        return servicePaymentRepository.save(payment);
    }
}
