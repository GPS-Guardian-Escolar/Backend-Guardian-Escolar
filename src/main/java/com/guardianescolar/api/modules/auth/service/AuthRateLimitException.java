package com.guardianescolar.api.modules.auth.service;

public class AuthRateLimitException extends IllegalArgumentException {

    public AuthRateLimitException() {
        super("Demasiados intentos. Solicita un nuevo codigo o espera antes de volver a intentar.");
    }
}
