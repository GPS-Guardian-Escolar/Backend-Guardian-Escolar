package com.guardianescolar.api.modules.maps.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class MapsDtos {

    private MapsDtos() {
    }

    public record GeocodeRequest(
            @NotBlank @Size(max = 500) String address,
            @Size(max = 10) String language,
            @Size(max = 10) String region) {
    }

    public record ReverseGeocodeRequest(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @Size(max = 10) String language) {
    }

    public record DirectionsRequest(
            @NotNull @Valid LocationInput origin,
            @NotNull @Valid LocationInput destination,
            @Size(max = 20) String mode,
            OffsetDateTime departureTime,
            @Size(max = 10) String language) {
    }

    public record LocationInput(
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @Size(max = 500) String address) {
    }

    public record GeocodeResponse(
            String status,
            boolean cached,
            List<GeocodeResult> results) {
    }

    public record GeocodeResult(
            String formattedAddress,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            List<String> types) {
    }

    public record DirectionsResponse(
            String status,
            boolean cached,
            List<DirectionRoute> routes) {
    }

    public record DirectionRoute(
            String summary,
            Integer distanceMeters,
            Integer durationSeconds,
            Integer durationInTrafficSeconds,
            String overviewPolyline) {
    }

    public record PlaceAutocompleteResponse(
            String status,
            boolean cached,
            List<PlacePrediction> predictions) {
    }

    public record PlacePrediction(
            String description,
            String placeId,
            List<String> types) {
    }

    public record GooglePayload(
            String status,
            JsonNode payload) {
    }
}
