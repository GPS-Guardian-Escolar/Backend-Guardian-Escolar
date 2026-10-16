package com.guardianescolar.api.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardianescolar.api.modules.maps.config.GoogleMapsProperties;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.PlaceAutocompleteResponse;
import com.guardianescolar.api.modules.maps.exception.MapsApiException;
import com.guardianescolar.api.modules.maps.service.GoogleMapsClient;
import com.guardianescolar.api.modules.maps.service.MapsCacheService;
import com.guardianescolar.api.modules.maps.service.MapsKeyService;
import com.guardianescolar.api.modules.maps.service.MapsMetricsService;
import com.guardianescolar.api.modules.maps.service.MapsService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PlacesApiNewTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postsPlacesNewRequestWithKeyAndSessionToken() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/places:autocomplete", exchange -> {
            method.set(exchange.getRequestMethod());
            apiKey.set(exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            write(exchange, 200, "{\"suggestions\":[]}");
        });
        server.start();

        try {
            GoogleMapsClient client = client("test-key", server.getAddress().getPort(), mock(MapsMetricsService.class));

            JsonNode response = client.autocomplete("Neiva Huila", "es", "CO", "session-123");

            assertEquals("POST", method.get());
            assertEquals("test-key", apiKey.get());
            assertEquals(0, response.path("suggestions").size());
            JsonNode sent = objectMapper.readTree(body.get());
            assertEquals("Neiva Huila", sent.path("input").asText());
            assertEquals("es", sent.path("languageCode").asText());
            assertEquals("CO", sent.path("regionCode").asText());
            assertEquals("session-123", sent.path("sessionToken").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsPlaceAndQuerySuggestionsToExistingContract() throws Exception {
        GoogleMapsClient client = mock(GoogleMapsClient.class);
        MapsCacheService cache = mock(MapsCacheService.class);
        MapsKeyService keyService = mock(MapsKeyService.class);
        MapsMetricsService metrics = mock(MapsMetricsService.class);
        when(keyService.hash(any())).thenReturn("cache-key");
        when(cache.getGeocoding("cache-key")).thenReturn(Optional.empty());
        when(client.autocomplete("Neiva", "es", "CO", "session-123"))
                .thenReturn(objectMapper.readTree("""
                        {
                          "suggestions": [
                            {"placePrediction": {
                              "place": "places/abc123",
                              "placeId": "abc123",
                              "text": {"text": "SENA Neiva"},
                              "types": ["school"]
                            }},
                            {"queryPrediction": {
                              "text": {"text": "colegios en Neiva"}
                            }}
                          ]
                        }
                        """));

        MapsService service = new MapsService(client, cache, keyService, metrics, objectMapper);

        PlaceAutocompleteResponse response = service.autocomplete("Neiva", "session-123");

        assertEquals("OK", response.status());
        assertEquals(2, response.predictions().size());
        assertEquals("SENA Neiva", response.predictions().get(0).description());
        assertEquals("abc123", response.predictions().get(0).placeId());
        assertEquals("school", response.predictions().get(0).types().get(0));
        assertEquals("colegios en Neiva", response.predictions().get(1).description());
        assertEquals(null, response.predictions().get(1).placeId());
        verify(client).autocomplete("Neiva", "es", "CO", "session-123");
    }

    @Test
    void reportsMissingApiKeyWithoutCallingProvider() throws Exception {
        MapsMetricsService metrics = mock(MapsMetricsService.class);
        GoogleMapsClient client = client("", 1, metrics);

        MapsApiException exception = assertThrows(MapsApiException.class,
                () -> client.autocomplete("Neiva", "es", "CO", null));

        assertEquals(503, exception.status().value());
        assertEquals("MISSING_API_KEY", exception.googleStatus());
        verify(metrics).recordError("MISSING_API_KEY");
    }

    @Test
    void mapsProviderRateLimitWithoutExposingProviderBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/places:autocomplete", exchange ->
                write(exchange, 429, "private provider details"));
        server.start();

        try {
            MapsMetricsService metrics = mock(MapsMetricsService.class);
            GoogleMapsClient client = client("test-key", server.getAddress().getPort(), metrics);

            MapsApiException exception = assertThrows(MapsApiException.class,
                    () -> client.autocomplete("Neiva", "es", "CO", null));

            assertEquals(429, exception.status().value());
            assertEquals("PLACES_RATE_LIMITED", exception.googleStatus());
            org.junit.jupiter.api.Assertions.assertFalse(
                    exception.getMessage().contains("private provider details"));
            verify(metrics).recordError("PLACES_RATE_LIMITED");
        } finally {
            server.stop(0);
        }
    }

    private GoogleMapsClient client(String apiKey, int placesPort, MapsMetricsService metrics) {
        GoogleMapsProperties properties = new GoogleMapsProperties(
                apiKey,
                "http://localhost:1/maps/api",
                "http://localhost:1/routes",
                "http://localhost:" + placesPort,
                1000,
                1000,
                new GoogleMapsProperties.Cache(Duration.ofHours(1), Duration.ofMinutes(1), 100),
                new GoogleMapsProperties.Resilience(1, Duration.ZERO, 50, 10),
                new GoogleMapsProperties.RateLimit(60));
        RestClient maps = RestClient.builder().baseUrl(properties.baseUrl()).build();
        RestClient routes = RestClient.builder().baseUrl(properties.routesBaseUrl()).build();
        RestClient places = RestClient.builder().baseUrl(properties.placesBaseUrl()).build();
        return new GoogleMapsClient(
                maps,
                routes,
                places,
                properties,
                objectMapper,
                Retry.of("places-test-" + System.nanoTime(), io.github.resilience4j.retry.RetryConfig.custom()
                        .maxAttempts(1).build()),
                CircuitBreaker.ofDefaults("places-test-" + System.nanoTime()),
                metrics);
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
