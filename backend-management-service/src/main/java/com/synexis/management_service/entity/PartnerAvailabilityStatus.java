package com.synexis.management_service.entity;

/**
 * Matches {@code Partner.availability_status} in the logical schema
 * (SQLBD.sql).
 */
public enum PartnerAvailabilityStatus {
    available,
    busy,
    disabled
}
