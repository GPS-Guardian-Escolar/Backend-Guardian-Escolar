package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class RegistrationValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valid(String name, String phone) {
        return validator.validate(new AuthDtos.RegisterRequest(
                name, "ana@example.com", phone, "Password1!", true, null, null)).isEmpty();
    }

    @Test
    void acceptsNormalColombianContact() {
        assertTrue(valid("Ana María", "3001234567"));
    }

    @Test
    void rejectsExcessiveOrRepeatedNames() {
        assertFalse(valid("A", "3001234567"));
        assertFalse(valid("An", "3001234567"));
        assertFalse(valid("A".repeat(61), "3001234567"));
        assertFalse(valid("Annnna", "3001234567"));
        assertFalse(valid("aaa", "3001234567"));
        assertTrue(valid("Alberto", "3001234567"));
        assertFalse(valid("Ana123", "3001234567"));
    }

    @Test
    void rejectsFakePhoneNumber() {
        assertFalse(valid("Ana María", "3333333333"));
        assertFalse(valid("Ana María", "4444444444"));
        assertFalse(valid("Ana María", "3222222222"));
        assertFalse(valid("Ana María", "3777777777"));
        assertFalse(valid("Ana María", "3000000000"));
        assertFalse(valid("Ana María", "3999999999"));
        assertFalse(valid("Ana María", "3444444444"));
        assertFalse(valid("Ana María", "3455555555"));
        assertFalse(valid("Ana María", "3111111111"));
        assertTrue(valid("Ana María", "3221234567"));
    }
}
