package com.guardianescolar.api.modules.routes.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OsrmRoutesClient {

    private static final String DEFAULT_BASE_URL = "https://router.project-osrm.org";

    private final RestClient restClient;

    public OsrmRoutesClient(
            RestClient.Builder restClientBuilder,
            @Value("${guardian.osrm.base-url:${OSRM_BASE_URL:https://router.project-osrm.org}}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim())
                .build();
    }

    public Geometry calculate(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            List<Intermediate> intermediates,
            BigDecimal destinationLatitude,
            BigDecimal destinationLongitude) {
        List<String> coordinates = new ArrayList<>();
        coordinates.add(formatWaypoint(originLongitude, originLatitude));
        if (intermediates != null) {
            intermediates.stream()
                    .map(item -> formatWaypoint(item.longitude(), item.latitude()))
                    .forEach(coordinates::add);
        }
        coordinates.add(formatWaypoint(destinationLongitude, destinationLatitude));

        String uri = "/route/v1/driving/" + String.join(";", coordinates)
                + "?overview=full&geometries=polyline&steps=false";

        JsonNode response;
        try {
            response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new IllegalStateException(
                    "OSRM HTTP error " + e.getStatusCode()
                            + (body == null || body.isBlank() ? "" : ": " + body),
                    e);
        } catch (RestClientException e) {
            throw new IllegalStateException("OSRM no pudo calcular la geometría.", e);
        }

        String code = response == null ? null : response.path("code").asText(null);
        if (response == null || "NoRoute".equalsIgnoreCase(code)
                || !response.path("routes").isArray() || response.path("routes").size() == 0) {
            String resolvedCode = code == null ? "UNKNOWN" : code;
            String message = response == null ? "respuesta vacía" : response.path("message").asText(resolvedCode);
            throw new IllegalStateException(
                    "OSRM no devolvió una ruta válida (" + resolvedCode + "): " + message);
        }

        JsonNode route = response.path("routes").get(0);
        if (route == null || route.isMissingNode()) {
            throw new IllegalStateException("OSRM no devolvió una ruta válida.");
        }

        String encodedPolyline = route.path("geometry").asText(null);
        double distanceMeters = route.path("distance").asDouble(-1d);
        double durationSeconds = route.path("duration").asDouble(-1d);
        if (encodedPolyline == null || encodedPolyline.isBlank()
                || distanceMeters < 0 || durationSeconds < 0) {
            throw new IllegalStateException("La respuesta de OSRM está incompleta.");
        }

        return new Geometry(
                encodedPolyline,
                (int) Math.round(distanceMeters),
                Math.round((float) durationSeconds));
    }

    private static String formatWaypoint(BigDecimal longitude, BigDecimal latitude) {
        return String.format(
                Locale.ROOT,
                "%s,%s",
                normalize(longitude),
                normalize(latitude));
    }

    private static String normalize(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    public record Intermediate(BigDecimal latitude, BigDecimal longitude) {
    }

    public record Geometry(String encodedPolyline, int distanceMeters, long durationSeconds) {
    }
}
