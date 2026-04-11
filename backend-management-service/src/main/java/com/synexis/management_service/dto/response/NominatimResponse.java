package com.synexis.management_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class NominatimResponse {

    private AddressDetails address;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressDetails {
        private String neighbourhood;
        private String city;
        private String state;
        private String country;
        private String country_code;

    }
}
