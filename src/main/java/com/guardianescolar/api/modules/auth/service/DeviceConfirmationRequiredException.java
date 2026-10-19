package com.guardianescolar.api.modules.auth.service;

public class DeviceConfirmationRequiredException extends IllegalArgumentException {

    public static final String CODE = "DEVICE_CONFIRMATION_REQUIRED";

    public DeviceConfirmationRequiredException(String message) {
        super(message);
    }
}
