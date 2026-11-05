package com.guardianescolar.api.modules.auth.service;

public class EmailUnavailableException extends RuntimeException {
    public EmailUnavailableException() {
        super("El servicio de correo no está disponible en este momento");
    }
}
