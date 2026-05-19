package com.synexis.management_service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class WikimediaClient {

    private final WebClient webClient;

    public WikimediaClient(@Qualifier("wikimediaWebClient") WebClient wikimediaWebClient) {
        this.webClient = wikimediaWebClient;
    }

    /**
     * Returns an image URL of the nearest Wikimedia Commons file
     * to the given coordinates using geosearch. Returns null if no result is found
     * or any
     * call fails, so the service completion is never blocked.
     */
    public String getLocationImageUrl(double lat, double lon) {
        try {
            // Query Wikimedia Commons using geosearch to find images near the location
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/w/api.php")
                            .queryParam("action", "query")
                            .queryParam("generator", "geosearch")
                            .queryParam("ggscoord", lat + "|" + lon)
                            .queryParam("ggsnamespace", "6")
                            .queryParam("ggsradius", "1000")
                            .queryParam("ggslimit", "5")
                            .queryParam("prop", "imageinfo")
                            .queryParam("iiprop", "url")
                            .queryParam("iiurlwidth", "600")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null)
                return null;

            Map<String, Object> query = (Map<String, Object>) response.get("query");
            if (query == null)
                return null;

            Map<String, Object> pages = (Map<String, Object>) query.get("pages");
            if (pages == null || pages.isEmpty())
                return null;

            // Get the first page entry
            Map<String, Object> firstPage = (Map<String, Object>) pages.values().iterator().next();
            if (firstPage == null)
                return null;

            List<Map<String, Object>> imageinfo = (List<Map<String, Object>>) firstPage.get("imageinfo");
            if (imageinfo == null || imageinfo.isEmpty())
                return null;

            String rawUrl = (String) imageinfo.get(0).get("url");
            if (rawUrl == null)
                return null;
            return rawUrl.contains("?") ? rawUrl.substring(0, rawUrl.indexOf("?")) : rawUrl;

        } catch (Exception e) {
            // Never block service completion due to image retrieval failure
            return null;
        }
    }
}
