package com.guardianescolar.api.modules.notifications.service;

import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SmsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsService.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String provider;
    private final String providerUrl;
    private final String apiKey;
    private final String from;

    public SmsService(
            RestClient.Builder restClientBuilder,
            @Value("${guardian.sms.enabled:false}") boolean enabled,
            @Value("${guardian.sms.provider:GENERIC}") String provider,
            @Value("${guardian.sms.provider-url:}") String providerUrl,
            @Value("${guardian.sms.api-key:}") String apiKey,
            @Value("${guardian.sms.from:GuardianEscolar}") String from) {
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.provider = provider;
        this.providerUrl = providerUrl;
        this.apiKey = apiKey;
        this.from = from;
    }

    public boolean isAvailable() {
        return enabled
                && providerUrl != null && !providerUrl.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    public boolean send(String phone, String message) {
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone == null) {
            LOGGER.info("SMS skipped because the user has no registered phone.");
            return false;
        }
        if (!isAvailable()) {
            LOGGER.info("SMS skipped because the provider is disabled or not fully configured.");
            return false;
        }

        try {
            TextbeltResponse response = restClient.post()
                    .uri(providerUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body(normalizedPhone, message))
                    .retrieve()
                    .body(TextbeltResponse.class);
            boolean sent = response != null && response.success();
            if (!sent) {
                LOGGER.warn("SMS not sent to {}. Provider response: {}", normalizedPhone, response);
            }
            return sent;
        } catch (RuntimeException error) {
            LOGGER.warn("Could not send SMS to {}", normalizedPhone, error);
            return false;
        }
    }

    public boolean sendToPhones(Set<String> phones, String message) {
        if (phones == null || phones.isEmpty()) {
            return false;
        }
        boolean sent = false;
        Set<String> uniquePhones = new LinkedHashSet<>();
        for (String phone : phones) {
            String normalized = normalizePhone(phone);
            if (normalized != null) {
                uniquePhones.add(normalized);
            }
        }
        for (String phone : uniquePhones) {
            sent = send(phone, message) || sent;
        }
        return sent;
    }

    private MultiValueMap<String, String> body(String phone, String message) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if ("TEXTBELT".equalsIgnoreCase(provider)) {
            body.add("phone", phone);
            body.add("message", message);
            body.add("key", apiKey);
            body.add("sender", from);
            return body;
        }

        body.add("to", phone);
        body.add("from", from);
        body.add("message", message);
        body.add("apiKey", apiKey);
        return body;
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.substring(1).replaceAll("\\D", "");
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() == 10 && digits.startsWith("3")) {
            return "+57" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("57")) {
            return "+" + digits;
        }
        return "+" + digits;
    }

    private record TextbeltResponse(
            boolean success,
            Integer quotaRemaining,
            String textId,
            String error) {
    }
}
