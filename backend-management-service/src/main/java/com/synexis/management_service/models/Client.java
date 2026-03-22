package com.synexis.management_service.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * End-user account. Logical model: row in {@code Client} referencing {@code User}; this service
 * stores {@link UserBase} columns in table {@code clients}. No extra columns beyond {@code User}
 * in SQLBD.sql.
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client extends UserBase {}
