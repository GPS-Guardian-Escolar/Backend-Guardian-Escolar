package com.guardianescolar.api.modules.routes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class RouteDtos {

    private RouteDtos() {
    }

    public record RouteRequest(
            @NotBlank @Size(min = 2, max = 100) String routeName,
            @Size(max = 255) String description,
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal originLatitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal originLongitude,
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal destinationLatitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal destinationLongitude,
            List<@Valid StopRequest> stops) {
    }

    public record StopRequest(
            @NotNull @Positive Integer stopOrder,
            @NotBlank @Size(max = 100) String stopName,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude) {
    }

    public record RouteResponse(
            UUID id,
            String routeName,
            String description,
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            BigDecimal destinationLatitude,
            BigDecimal destinationLongitude,
            List<StopResponse> stops) {
    }

    public record StopResponse(
            UUID id,
            Integer stopOrder,
            String stopName,
            BigDecimal latitude,
            BigDecimal longitude) {
    }
}
