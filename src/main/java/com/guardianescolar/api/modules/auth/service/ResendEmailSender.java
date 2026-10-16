package com.guardianescolar.api.modules.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResendEmailSender implements EmailSender {

    private static final String DEFAULT_API_URL = "https://api.resend.com";

    private final String apiKey;
    private final URI apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ResendEmailSender(
            @Value("${guardian.mail.resend.api-key:}") String apiKey,
            @Value("${guardian.mail.resend.api-url:" + DEFAULT_API_URL + "}") String apiUrl,
            ObjectMapper objectMapper) {
        this(apiKey, URI.create(apiUrl), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(), objectMapper);
    }

    ResendEmailSender(String apiKey, URI apiUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(String recipient, String from, String subject, String text, String html) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY es obligatorio cuando MAIL_PROVIDER=resend");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", from);
        payload.put("to", List.of(recipient));
        payload.put("subject", subject);
        payload.put("text", text);
        payload.put("html", html);

        try {
            HttpRequest request = HttpRequest.newBuilder(apiUrl.resolve("/emails"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Resend rechazo el email con HTTP " + response.statusCode());
            }
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo serializar el email para Resend", error);
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo conectar con Resend", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La solicitud a Resend fue interrumpida", error);
        }
    }
}
