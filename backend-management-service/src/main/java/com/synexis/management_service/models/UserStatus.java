package com.synexis.management_service.models;

/** Matches {@code User.status} in the logical schema (SQLBD.sql). */
public enum UserStatus {
    active,
    suspended,
    deleted
}
