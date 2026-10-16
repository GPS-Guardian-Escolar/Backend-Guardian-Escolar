package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailLinkServiceTest {

    @Test
    void buildsVerificationLinkFromTrustedReturnUrlOrigin() {
        EmailLinkService service = new EmailLinkService();
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "https://frontend.example.com");

        String link = service.emailVerificationUrl("verification-token", "https://app.example.com/login");

        assertEquals(
                "https://app.example.com/verify-email?token=verification-token&returnUrl=https%3A%2F%2Fapp.example.com%2Flogin",
                link);
    }

    @Test
    void fallsBackToConfiguredFrontendForInvalidReturnUrl() {
        EmailLinkService service = new EmailLinkService();
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "https://frontend.example.com");

        assertEquals(
                "https://frontend.example.com/verify-email?token=verification-token",
                service.emailVerificationUrl("verification-token", "javascript:alert(1)"));
    }
}
