package com.guardianescolar.api.modules.maps.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class MapsMetricsService {

    private final MeterRegistry meterRegistry;

    public MapsMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String endpoint, boolean cached) {
        meterRegistry.counter("google_maps_requests_total",
                "endpoint", endpoint,
                "cached", Boolean.toString(cached)).increment();
    }

    public void recordLatency(String endpoint, Duration duration) {
        Timer.builder("google_maps_latency_seconds")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordError(String code) {
        meterRegistry.counter("google_maps_errors_total",
                "code", code == null ? "UNKNOWN" : code).increment();
    }
}
