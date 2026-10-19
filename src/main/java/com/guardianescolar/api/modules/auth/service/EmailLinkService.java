package com.guardianescolar.api.modules.auth.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailLinkService {

    @Value("${guardian.mail.frontend-base-url:http://localhost:8081}")
    private String frontendBaseUrl;

    public String emailVerificationUrl(String token, String returnUrl) {
        return frontendOriginFromReturnUrl(returnUrl) + "/verify-email?token=" + token + returnUrlParam(returnUrl);
    }

    public String passwordResetUrl(String token) {
        return frontendBaseUrl + "/reset-password?token=" + token;
    }

    public String deviceConfirmationUrl(String token, String returnUrl) {
        return frontendOriginFromReturnUrl(returnUrl) + "/confirm-login?token=" + token + returnUrlParam(returnUrl);
    }

    public String frontendBaseUrl() {
        return frontendBaseUrl;
    }

    private String returnUrlParam(String returnUrl) {
        if (!isSafeReturnUrl(returnUrl)) {
            return "";
        }
        return "&returnUrl=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
    }

    private String frontendOriginFromReturnUrl(String returnUrl) {
        if (!isSafeReturnUrl(returnUrl)) {
            return frontendBaseUrl;
        }
        try {
            URI uri = URI.create(returnUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return frontendBaseUrl;
            }
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (IllegalArgumentException error) {
            return frontendBaseUrl;
        }
    }

    private boolean isSafeReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return false;
        }
        if (returnUrl.startsWith("/") && !returnUrl.startsWith("//")) {
            return true;
        }
        try {
            URI uri = URI.create(returnUrl);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}
