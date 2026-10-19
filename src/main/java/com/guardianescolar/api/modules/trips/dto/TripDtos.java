package com.guardianescolar.api.modules.trips.dto;

import com.guardianescolar.api.modules.trips.domain.TripStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class TripDtos {

    private TripDtos() {
    }

        public record TripRequest(
                @NotNull UUID studentId,
                UUID routeId,
                @PastOrPresent OffsetDateTime tripStartedAt) {
    }

    public record ChangeStatusRequest(
            @NotNull TripStatus status,
            Boolean hadDeviation) {
    }

    public record CoordinateRequest(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @PastOrPresent OffsetDateTime recordedAt,
            @PositiveOrZero @DecimalMax("200.0") BigDecimal speedKmh,
            @PositiveOrZero @Max(86400) Integer stoppedSeconds) {
    }

    public record TripResponse(
            UUID id,
            UUID studentId,
            String student,
            UUID routeId,
            String route,
            OffsetDateTime tripStartedAt,
            OffsetDateTime tripEndedAt,
            TripStatus status,
            Boolean hadDeviation) {
    }

    public record CoordinateResponse(
            UUID id,
            UUID tripId,
            BigDecimal latitude,
            BigDecimal longitude,
            OffsetDateTime recordedAt,
            BigDecimal speedKmh,
            Integer stoppedSeconds) {
    }
}
