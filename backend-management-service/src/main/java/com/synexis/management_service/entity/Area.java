package com.synexis.management_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Area entity representing geographical areas.
 */
@Embeddable
@Getter
@Setter
public class Area {

    @Column(nullable = false)
    private String neighborhood;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

}