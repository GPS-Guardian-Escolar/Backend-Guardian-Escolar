package com.guardianescolar.api.modules.maps.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "google.maps")
public record GoogleMapsProperties(
        String apiKey,
        @NotBlank String baseUrl,
        @Positive int connectTimeoutMillis,
        @Positive int readTimeoutMillis,
        Cache cache,
        Resilience resilience,
        RateLimit rateLimit) {

    public record Cache(
            Duration geocodingTtl,
            Duration directionsTtl,
            long maximumSize) {
    }

    public record Resilience(
            int retryAttempts,
            Duration retryWait,
            int circuitBreakerFailureRateThreshold,
            int circuitBreakerSlidingWindowSize) {
    }

    public record RateLimit(
            int permitsPerMinute) {
    }
}
