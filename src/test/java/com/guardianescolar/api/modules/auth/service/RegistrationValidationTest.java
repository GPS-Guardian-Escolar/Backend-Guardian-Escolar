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
        assertFalse(valid("A".repeat(61), "3001234567"));
        assertFalse(valid("Annnna", "3001234567"));
    }

    @Test
    void rejectsFakePhoneNumber() {
        assertFalse(valid("Ana María", "3333333333"));
        assertFalse(valid("Ana María", "4444444444"));
    }
}
