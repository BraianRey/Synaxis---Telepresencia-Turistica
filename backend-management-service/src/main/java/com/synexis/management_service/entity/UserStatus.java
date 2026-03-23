package com.synexis.management_service.entity;

/** Matches {@code User.status} in the logical schema (SQLBD.sql). */
public enum UserStatus {
    active,
    suspended,
    deleted
}
