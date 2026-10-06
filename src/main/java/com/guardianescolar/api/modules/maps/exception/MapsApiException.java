package com.guardianescolar.api.modules.maps.exception;

import java.time.Duration;
import org.springframework.http.HttpStatus;

public class MapsApiException extends RuntimeException {

    private final HttpStatus status;
    private final String googleStatus;
    private final Duration retryAfter;

    public MapsApiException(HttpStatus status, String googleStatus, String message, Duration retryAfter) {
        super(message);
        this.status = status;
        this.googleStatus = googleStatus;
        this.retryAfter = retryAfter;
    }

    public HttpStatus status() {
        return status;
    }

    public String googleStatus() {
        return googleStatus;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
