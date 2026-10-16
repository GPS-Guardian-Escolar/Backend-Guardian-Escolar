package com.guardianescolar.api.modules.maps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardianescolar.api.modules.maps.config.GoogleMapsProperties;
import com.guardianescolar.api.modules.maps.exception.MapsApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.LocationInput;

@Service
public class GoogleMapsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleMapsClient.class);

    private final RestClient restClient;
    private final RestClient routesRestClient;
    private final RestClient placesRestClient;
    private final GoogleMapsProperties properties;
    private final ObjectMapper objectMapper;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final MapsMetricsService metricsService;

    public GoogleMapsClient(
            RestClient googleMapsRestClient,
            RestClient googleRoutesRestClient,
            RestClient googlePlacesRestClient,
            GoogleMapsProperties properties,
            ObjectMapper objectMapper,
            Retry googleMapsRetry,
            CircuitBreaker googleMapsCircuitBreaker,
            MapsMetricsService metricsService) {
        this.restClient = googleMapsRestClient;
        this.routesRestClient = googleRoutesRestClient;
        this.placesRestClient = googlePlacesRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.retry = googleMapsRetry;
        this.circuitBreaker = googleMapsCircuitBreaker;
        this.metricsService = metricsService;
    }

    public JsonNode get(String endpoint, String uriTemplate, Object... uriVariables) {
        if (!StringUtils.hasText(properties.apiKey())) {
            metricsService.recordError("MISSING_API_KEY");
            throw new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "MISSING_API_KEY",
                    "La llave de servidor de Google Maps no está configurada", Duration.ofMinutes(5));
        }
        Instant startedAt = Instant.now();
        Supplier<JsonNode> supplier = () -> execute(endpoint, uriTemplate, uriVariables);
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier)).get();
        } finally {
            Duration latency = Duration.between(startedAt, Instant.now());
            metricsService.recordLatency(endpoint, latency);
            LOGGER.info("Google Maps call endpoint={} latencyMs={} quotaUnits=1 cache=miss",
                    endpoint, latency.toMillis());
        }
    }

    public JsonNode computeRoute(LocationInput origin, LocationInput destination, String mode, String language) {
        if (!StringUtils.hasText(properties.apiKey())) {
            metricsService.recordError("MISSING_API_KEY");
            throw new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "MISSING_API_KEY",
                    "La llave de servidor de Google Maps no está configurada", Duration.ofMinutes(5));
        }
        Instant startedAt = Instant.now();
        Supplier<JsonNode> supplier = () -> executeRoute(origin, destination, mode, language);
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier)).get();
        } finally {
            Duration latency = Duration.between(startedAt, Instant.now());
            metricsService.recordLatency("directions", latency);
            LOGGER.info("Google Maps call endpoint=routes-v2 latencyMs={} quotaUnits=1 cache=miss",
                    latency.toMillis());
        }
    }

    public JsonNode autocomplete(String input, String language, String region, String sessionToken) {
        if (!StringUtils.hasText(properties.apiKey())) {
            metricsService.recordError("MISSING_API_KEY");
            throw new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "MISSING_API_KEY",
                    "La llave de servidor de Google Maps no está configurada", Duration.ofMinutes(5));
        }
        Instant startedAt = Instant.now();
        Supplier<JsonNode> supplier = () -> executeAutocomplete(input, language, region, sessionToken);
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier)).get();
        } finally {
            Duration latency = Duration.between(startedAt, Instant.now());
            metricsService.recordLatency("places-autocomplete", latency);
            LOGGER.info("Google Maps call endpoint=places-autocomplete-new latencyMs={} quotaUnits=1 cache=miss",
                    latency.toMillis());
        }
    }

    private JsonNode executeRoute(LocationInput origin, LocationInput destination, String mode, String language) {
        try {
            var request = objectMapper.createObjectNode();
            request.set("origin", routeLocation(origin));
            request.set("destination", routeLocation(destination));
            request.put("travelMode", "driving".equalsIgnoreCase(mode) ? "DRIVE" : "DRIVE");
            request.put("languageCode", StringUtils.hasText(language) ? language : "es");
            request.put("polylineEncoding", "ENCODED_POLYLINE");
            String body = routesRestClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.description,routes.polyline.encodedPolyline")
                    .body(request)
                    .retrieve()
                    .body(String.class);
            JsonNode payload = objectMapper.readTree(body);
            if (!payload.path("routes").isArray() || payload.path("routes").isEmpty()) {
                throw mapGoogleError("ZERO_RESULTS", "Google Routes no encontró un trayecto");
            }
            return payload;
        } catch (MapsApiException exception) {
            metricsService.recordError(exception.googleStatus());
            throw exception;
        } catch (Exception exception) {
            metricsService.recordError("ROUTES_API_ERROR");
            throw new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "ROUTES_API_ERROR",
                    "No fue posible calcular el trayecto con Google Routes", Duration.ofSeconds(30));
        }
    }

    private com.fasterxml.jackson.databind.node.ObjectNode routeLocation(LocationInput input) {
        var location = objectMapper.createObjectNode();
        if (input.latitude() != null && input.longitude() != null) {
            var latLng = objectMapper.createObjectNode();
            latLng.put("latitude", input.latitude());
            latLng.put("longitude", input.longitude());
            location.set("location", objectMapper.createObjectNode().set("latLng", latLng));
        } else {
            location.put("address", input.address());
        }
        return location;
    }

    private JsonNode execute(String endpoint, String uriTemplate, Object... uriVariables) {
        String body = restClient.get()
                .uri(uriTemplate + "&key={key}", appendKey(uriVariables))
                .retrieve()
                .body(String.class);
        try {
            JsonNode payload = objectMapper.readTree(body);
            String status = payload.path("status").asText("UNKNOWN");
            if (!"OK".equals(status)) {
                throw mapGoogleError(status, payload.path("error_message").asText("Error consultando Google Maps"));
            }
            return payload;
        } catch (MapsApiException exception) {
            metricsService.recordError(exception.googleStatus());
            throw exception;
        } catch (Exception exception) {
            metricsService.recordError("PARSE_ERROR");
            throw new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "PARSE_ERROR",
                    "No fue posible procesar la respuesta de Google Maps", Duration.ofSeconds(30));
        }
    }

    private JsonNode executeAutocomplete(String input, String language, String region, String sessionToken) {
        try {
            var request = objectMapper.createObjectNode();
            request.put("input", input);
            if (StringUtils.hasText(language)) {
                request.put("languageCode", language);
            }
            if (StringUtils.hasText(region)) {
                request.put("regionCode", region);
            }
            if (StringUtils.hasText(sessionToken)) {
                request.put("sessionToken", sessionToken);
            }
            String body = placesRestClient.post()
                    .uri("/v1/places:autocomplete")
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (RestClientResponseException exception) {
            MapsApiException mapped = mapHttpError(exception.getStatusCode().value());
            metricsService.recordError(mapped.googleStatus());
            throw mapped;
        } catch (Exception exception) {
            metricsService.recordError("PLACES_API_ERROR");
            throw new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLACES_API_ERROR",
                    "No fue posible consultar Google Places", Duration.ofSeconds(30));
        }
    }

    private MapsApiException mapHttpError(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        return switch (statusCode) {
            case 400 -> new MapsApiException(HttpStatus.BAD_REQUEST, "PLACES_BAD_REQUEST",
                    "Solicitud inválida para Google Places", null);
            case 401 -> new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLACES_UNAUTHORIZED",
                    "Google Places rechazó la autenticación del servidor", Duration.ofMinutes(5));
            case 403 -> new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLACES_FORBIDDEN",
                    "Google Places rechazó la solicitud del servidor", Duration.ofMinutes(5));
            case 404 -> new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLACES_NOT_FOUND",
                    "El recurso de Google Places no está disponible", Duration.ofSeconds(30));
            case 429 -> new MapsApiException(HttpStatus.TOO_MANY_REQUESTS, "PLACES_RATE_LIMITED",
                    "Cuota de Google Places excedida", Duration.ofSeconds(60));
            default -> new MapsApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PLACES_HTTP_" + statusCode,
                    "Google Places no pudo completar la solicitud", Duration.ofSeconds(30));
        };
    }

    private Object[] appendKey(Object[] uriVariables) {
        Object[] values = new Object[uriVariables.length + 1];
        System.arraycopy(uriVariables, 0, values, 0, uriVariables.length);
        values[uriVariables.length] = properties.apiKey();
        return values;
    }

    private MapsApiException mapGoogleError(String status, String message) {
        return switch (status) {
            case "OVER_QUERY_LIMIT" -> new MapsApiException(HttpStatus.TOO_MANY_REQUESTS, status,
                    "Cuota de Google Maps excedida", Duration.ofSeconds(60));
            case "REQUEST_DENIED" -> new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, status,
                    "Google Maps rechazó la solicitud del servidor", Duration.ofMinutes(5));
            case "ZERO_RESULTS" -> new MapsApiException(HttpStatus.BAD_REQUEST, status,
                    "Google Maps no encontró resultados para la solicitud", null);
            case "INVALID_REQUEST" -> new MapsApiException(HttpStatus.BAD_REQUEST, status,
                    "Solicitud inválida para Google Maps: " + message, null);
            default -> new MapsApiException(HttpStatus.SERVICE_UNAVAILABLE, status,
                    "Google Maps no pudo completar la solicitud", Duration.ofSeconds(30));
        };
    }
}
