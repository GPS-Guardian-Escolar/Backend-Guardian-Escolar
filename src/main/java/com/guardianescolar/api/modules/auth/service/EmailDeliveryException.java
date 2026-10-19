package com.guardianescolar.api.modules.auth.service;

/**
 * Indicates that an email provider rejected or could not process a message.
 * The public API must not expose provider response bodies or credentials.
 */
public class EmailDeliveryException extends RuntimeException {

    private final String provider;
    private final Integer providerStatus;

    public EmailDeliveryException(String provider, Integer providerStatus, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.providerStatus = providerStatus;
    }

    public String provider() {
        return provider;
    }

    public Integer providerStatus() {
        return providerStatus;
    }
}
