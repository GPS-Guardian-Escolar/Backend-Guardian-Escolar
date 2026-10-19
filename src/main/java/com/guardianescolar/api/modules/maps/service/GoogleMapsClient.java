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

@Service
public class GoogleMapsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleMapsClient.class);

    private final RestClient restClient;
    private final GoogleMapsProperties properties;
    private final ObjectMapper objectMapper;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final MapsMetricsService metricsService;

    public GoogleMapsClient(
            RestClient googleMapsRestClient,
            GoogleMapsProperties properties,
            ObjectMapper objectMapper,
            Retry googleMapsRetry,
            CircuitBreaker googleMapsCircuitBreaker,
            MapsMetricsService metricsService) {
        this.restClient = googleMapsRestClient;
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
