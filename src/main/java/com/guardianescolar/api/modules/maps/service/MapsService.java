package com.guardianescolar.api.modules.maps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardianescolar.api.modules.maps.dto.MapsDtos;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.DirectionRoute;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.DirectionsRequest;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.DirectionsResponse;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.GeocodeRequest;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.GeocodeResponse;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.GeocodeResult;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.LocationInput;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.PlaceAutocompleteResponse;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.PlacePrediction;
import com.guardianescolar.api.modules.maps.dto.MapsDtos.ReverseGeocodeRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MapsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapsService.class);

    private final GoogleMapsClient googleMapsClient;
    private final MapsCacheService cacheService;
    private final MapsKeyService keyService;
    private final MapsMetricsService metricsService;
    private final ObjectMapper objectMapper;

    public MapsService(
            GoogleMapsClient googleMapsClient,
            MapsCacheService cacheService,
            MapsKeyService keyService,
            MapsMetricsService metricsService,
            ObjectMapper objectMapper) {
        this.googleMapsClient = googleMapsClient;
        this.cacheService = cacheService;
        this.keyService = keyService;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    public GeocodeResponse geocode(GeocodeRequest request) {
        String cacheKey = keyService.hash("geocode|" + request.address() + "|" + request.language() + "|" + request.region());
        return cacheService.getGeocoding(cacheKey)
                .map(payload -> readGeocode("geocode", payload, true))
                .orElseGet(() -> {
                    JsonNode payload = googleMapsClient.get("geocode",
                            "/geocode/json?address={address}&language={language}&region={region}",
                            request.address(), defaultText(request.language()), defaultText(request.region()));
                    cacheService.putGeocoding(cacheKey, write(payload));
                    return toGeocodeResponse("geocode", payload, false);
                });
    }

    public GeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        String latlng = request.latitude() + "," + request.longitude();
        String cacheKey = keyService.hash("reverse|" + latlng + "|" + request.language());
        return cacheService.getGeocoding(cacheKey)
                .map(payload -> readGeocode("reverse-geocode", payload, true))
                .orElseGet(() -> {
                    JsonNode payload = googleMapsClient.get("reverse-geocode",
                            "/geocode/json?latlng={latlng}&language={language}",
                            latlng, defaultText(request.language()));
                    cacheService.putGeocoding(cacheKey, write(payload));
                    return toGeocodeResponse("reverse-geocode", payload, false);
                });
    }

    public DirectionsResponse directions(DirectionsRequest request) {
        String cacheKey = keyService.hash("directions|" + locationValue(request.origin()) + "|"
                + locationValue(request.destination()) + "|" + request.mode() + "|" + request.departureTime() + "|"
                + request.language());
        return cacheService.getDirections(cacheKey)
                .map(payload -> readDirections(payload, true))
                .orElseGet(() -> {
                    JsonNode payload = googleMapsClient.get("directions",
                            "/directions/json?origin={origin}&destination={destination}&mode={mode}&departure_time={departureTime}&language={language}",
                            locationValue(request.origin()), locationValue(request.destination()),
                            defaultMode(request.mode()), departureTime(request.departureTime()),
                            defaultText(request.language()));
                    cacheService.putDirections(cacheKey, write(payload));
                    return toDirectionsResponse(payload, false);
                });
    }

    public PlaceAutocompleteResponse autocomplete(String input) {
        String cacheKey = keyService.hash("autocomplete|" + input);
        return cacheService.getGeocoding(cacheKey)
                .map(payload -> readAutocomplete(payload, true))
                .orElseGet(() -> {
                    JsonNode payload = googleMapsClient.get("places-autocomplete",
                            "/place/autocomplete/json?input={input}",
                            input);
                    cacheService.putGeocoding(cacheKey, write(payload));
                    return toAutocompleteResponse(payload, false);
                });
    }

    private GeocodeResponse readGeocode(String endpoint, String payload, boolean cached) {
        LOGGER.info("Google Maps cache hit endpoint={}", endpoint);
        return toGeocodeResponse(endpoint, read(payload), cached);
    }

    private DirectionsResponse readDirections(String payload, boolean cached) {
        LOGGER.info("Google Maps cache hit endpoint=directions");
        return toDirectionsResponse(read(payload), cached);
    }

    private PlaceAutocompleteResponse readAutocomplete(String payload, boolean cached) {
        LOGGER.info("Google Maps cache hit endpoint=places-autocomplete");
        return toAutocompleteResponse(read(payload), cached);
    }

    private GeocodeResponse toGeocodeResponse(String endpoint, JsonNode payload, boolean cached) {
        metricsService.recordRequest(endpoint, cached);
        List<GeocodeResult> results = new ArrayList<>();
        payload.path("results").forEach(result -> {
            JsonNode location = result.path("geometry").path("location");
            results.add(new GeocodeResult(
                    result.path("formatted_address").asText(null),
                    result.path("place_id").asText(null),
                    decimal(location.path("lat")),
                    decimal(location.path("lng")),
                    strings(result.path("types"))));
        });
        return new GeocodeResponse(payload.path("status").asText(), cached, results);
    }

    private DirectionsResponse toDirectionsResponse(JsonNode payload, boolean cached) {
        metricsService.recordRequest("directions", cached);
        List<DirectionRoute> routes = new ArrayList<>();
        payload.path("routes").forEach(route -> {
            int distanceMeters = 0;
            int durationSeconds = 0;
            int durationInTrafficSeconds = 0;
            for (JsonNode leg : route.path("legs")) {
                distanceMeters += leg.path("distance").path("value").asInt(0);
                durationSeconds += leg.path("duration").path("value").asInt(0);
                durationInTrafficSeconds += leg.path("duration_in_traffic").path("value").asInt(0);
            }
            routes.add(new DirectionRoute(
                    route.path("summary").asText(null),
                    distanceMeters,
                    durationSeconds,
                    durationInTrafficSeconds == 0 ? null : durationInTrafficSeconds,
                    route.path("overview_polyline").path("points").asText(null)));
        });
        return new DirectionsResponse(payload.path("status").asText(), cached, routes);
    }

    private PlaceAutocompleteResponse toAutocompleteResponse(JsonNode payload, boolean cached) {
        metricsService.recordRequest("places-autocomplete", cached);
        List<PlacePrediction> predictions = new ArrayList<>();
        payload.path("predictions").forEach(prediction -> predictions.add(new PlacePrediction(
                prediction.path("description").asText(null),
                prediction.path("place_id").asText(null),
                strings(prediction.path("types")))));
        return new PlaceAutocompleteResponse(payload.path("status").asText(), cached, predictions);
    }

    private String locationValue(LocationInput input) {
        if (input.latitude() != null && input.longitude() != null) {
            return input.latitude() + "," + input.longitude();
        }
        if (StringUtils.hasText(input.address())) {
            return input.address();
        }
        throw new IllegalArgumentException("La ubicación debe incluir latitud/longitud o dirección");
    }

    private String defaultMode(String mode) {
        return StringUtils.hasText(mode) ? mode : "driving";
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private String departureTime(OffsetDateTime departureTime) {
        return departureTime == null ? "now" : Long.toString(departureTime.toEpochSecond());
    }

    private BigDecimal decimal(JsonNode value) {
        return value.isMissingNode() || value.isNull() ? null : BigDecimal.valueOf(value.asDouble());
    }

    private List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private String write(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar la respuesta de Google Maps", exception);
        }
    }

    private JsonNode read(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible leer la respuesta cacheada de Google Maps", exception);
        }
    }
}
