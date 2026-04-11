package com.synexis.management_service.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.synexis.management_service.dto.response.NominatimResponse;
import com.synexis.management_service.entity.Area;

@Component
public class NominatimClient {

    private final WebClient webClient;

    public NominatimClient(WebClient nominatimWebClient) {
        this.webClient = nominatimWebClient;
    }

    public Area getAreaFromCoordinates(double lat, double lon) {
        NominatimResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reverse")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(NominatimResponse.class)
                .block(); // We use block() here for simplicity, but in a real application you might want
                          // to handle this asynchronously

        Area area = new Area();
        area.setNeighborhood(response.getAddress().getNeighbourhood());
        area.setCity(response.getAddress().getCity());
        area.setState(response.getAddress().getState());
        area.setCountry(response.getAddress().getCountry());
        area.setCountryCode(response.getAddress().getCountry_code());
        return area;
    }
}