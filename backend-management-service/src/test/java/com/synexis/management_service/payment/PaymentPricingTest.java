package com.synexis.management_service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PaymentPricingTest {

    @Test
    void minimumPackage_isFiveUsdForUpToThirtyMinutes() {
        assertEquals(new BigDecimal("5.00"), PaymentPricing.calculateTotalAmount(10));
        assertEquals(new BigDecimal("5.00"), PaymentPricing.calculateTotalAmount(30));
    }

    @Test
    void tier1_chargesMinutes31To60() {
        // 5 + 15 * 0.1667 = 7.5005 -> 7.50
        assertEquals(new BigDecimal("7.50"), PaymentPricing.calculateTotalAmount(45));
    }

    @Test
    void tier2_appliesFromMinute61() {
        // 5 + 30*0.1667 + 30*0.15 = 5 + 5.001 + 4.5 = 14.501 -> 14.50
        assertEquals(new BigDecimal("14.50"), PaymentPricing.calculateTotalAmount(90));
    }

    @Test
    void longSession_usesAllTiersUpTo180() {
        // 5 + 30*0.1667 + 60*0.15 + 60*0.1333 = 27.002 -> 27.00
        assertEquals(new BigDecimal("27.00"), PaymentPricing.calculateTotalAmount(180));
    }

    @Test
    void tier4_appliesBeyond240Minutes() {
        // 5 + 30*0.1667 + 60*0.15 + 120*0.1333 + 60*0.1167
        // = 5 + 5.001 + 9 + 15.996 + 7.002 = 41.999 -> 42.00
        assertEquals(new BigDecimal("42.00"), PaymentPricing.calculateTotalAmount(300));
    }

    @Test
    void billedMinutes_enforcesMinimum() {
        assertEquals(30, PaymentPricing.billedMinutes(5));
        assertEquals(45, PaymentPricing.billedMinutes(45));
    }
}
