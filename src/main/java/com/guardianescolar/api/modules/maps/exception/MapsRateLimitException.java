package com.guardianescolar.api.modules.maps.exception;

import java.time.Duration;

public class MapsRateLimitException extends RuntimeException {

    private final Duration retryAfter;

    public MapsRateLimitException(Duration retryAfter) {
        super("Límite de solicitudes del módulo de mapas excedido");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
