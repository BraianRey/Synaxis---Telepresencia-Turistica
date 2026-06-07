package com.synexis.management_service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simulated tiered pricing (USD, minutes as billing unit).
 *
 * <ul>
 *   <li>Minutes 1–30: flat {@value #MIN_PACKAGE_PRICE_USD} USD (minimum package)</li>
 *   <li>Minutes 31–60: {@value #TIER1_RATE_PER_MINUTE} USD/min</li>
 *   <li>Minutes 61–120: {@value #TIER2_RATE_PER_MINUTE} USD/min</li>
 *   <li>Minutes 121–240: {@value #TIER3_RATE_PER_MINUTE} USD/min</li>
 *   <li>Minutes 241+: {@value #TIER4_RATE_PER_MINUTE} USD/min</li>
 * </ul>
 */
public final class PaymentPricing {

    public static final int MIN_BILLING_MINUTES = 30;
    public static final BigDecimal MIN_PACKAGE_PRICE_USD = new BigDecimal("5.00");

    public static final BigDecimal TIER1_RATE_PER_MINUTE = new BigDecimal("0.1667");
    public static final BigDecimal TIER2_RATE_PER_MINUTE = new BigDecimal("0.1500");
    public static final BigDecimal TIER3_RATE_PER_MINUTE = new BigDecimal("0.1333");
    public static final BigDecimal TIER4_RATE_PER_MINUTE = new BigDecimal("0.1167");

    /** Base-tier rate (minutes 31–60); kept for API display compatibility. */
    public static final BigDecimal RATE_PER_MINUTE_USD = TIER1_RATE_PER_MINUTE;

    public static final int TIER1_END_MINUTE = 60;
    public static final int TIER2_END_MINUTE = 120;
    public static final int TIER3_END_MINUTE = 240;

    /** Equivalent hourly rate at base tier (~10 USD/h). */
    public static final double EQUIVALENT_HOURLY_RATE_USD = TIER1_RATE_PER_MINUTE
            .multiply(BigDecimal.valueOf(60))
            .doubleValue();

    private PaymentPricing() {
    }

    public static int billedMinutes(int actualDurationMin) {
        return Math.max(actualDurationMin, MIN_BILLING_MINUTES);
    }

    public static BigDecimal calculateTotalAmount(int billedMinutes) {
        if (billedMinutes <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = MIN_PACKAGE_PRICE_USD;

        total = total.add(tierCharge(billedMinutes, 31, TIER1_END_MINUTE, TIER1_RATE_PER_MINUTE));
        total = total.add(tierCharge(billedMinutes, 61, TIER2_END_MINUTE, TIER2_RATE_PER_MINUTE));
        total = total.add(tierCharge(billedMinutes, 121, TIER3_END_MINUTE, TIER3_RATE_PER_MINUTE));
        total = total.add(tierCharge(billedMinutes, 241, Integer.MAX_VALUE, TIER4_RATE_PER_MINUTE));

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Weighted average USD/min for the billed duration (stored on {@code ServicePayment}).
     */
    public static BigDecimal effectiveRatePerMinute(int billedMinutes) {
        if (billedMinutes <= 0) {
            return BigDecimal.ZERO;
        }
        return calculateTotalAmount(billedMinutes)
                .divide(BigDecimal.valueOf(billedMinutes), 6, RoundingMode.HALF_UP);
    }

    public static BigDecimal estimateTotalFromActualMinutes(int actualDurationMin) {
        int safeActual = Math.max(1, actualDurationMin);
        return calculateTotalAmount(billedMinutes(safeActual));
    }

    static int minutesInTier(int billedMinutes, int tierStartMinute, int tierEndMinuteInclusive) {
        if (billedMinutes < tierStartMinute) {
            return 0;
        }
        int effectiveEnd = Math.min(billedMinutes, tierEndMinuteInclusive);
        return effectiveEnd - tierStartMinute + 1;
    }

    private static BigDecimal tierCharge(
            int billedMinutes,
            int tierStartMinute,
            int tierEndMinuteInclusive,
            BigDecimal ratePerMinute) {
        int minutes = minutesInTier(billedMinutes, tierStartMinute, tierEndMinuteInclusive);
        if (minutes == 0) {
            return BigDecimal.ZERO;
        }
        return ratePerMinute.multiply(BigDecimal.valueOf(minutes));
    }
}
