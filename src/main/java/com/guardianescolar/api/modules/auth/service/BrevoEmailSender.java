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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrevoEmailSender implements EmailSender {

    private static final String DEFAULT_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String fromName;
    private final URI apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public BrevoEmailSender(
            @Value("${guardian.mail.brevo.api-key:}") String apiKey,
            @Value("${guardian.mail.from-name:}") String fromName,
            @Value("${guardian.mail.brevo.api-url:" + DEFAULT_API_URL + "}") String apiUrl,
            ObjectMapper objectMapper) {
        this(apiKey, fromName, URI.create(apiUrl), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(), objectMapper);
    }

    BrevoEmailSender(String apiKey, String fromName, URI apiUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.fromName = fromName;
        this.apiUrl = apiUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(String recipient, String from, String subject, String text, String html) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY es obligatorio cuando MAIL_PROVIDER=brevo");
        }

        Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("email", from);
        if (fromName != null && !fromName.isBlank()) {
            sender.put("name", fromName);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(Map.of("email", recipient)));
        payload.put("subject", subject);
        payload.put("htmlContent", html);
        payload.put("textContent", text);

        try {
            HttpRequest request = HttpRequest.newBuilder(apiUrl)
                    .timeout(Duration.ofSeconds(20))
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Brevo rechazo el email con HTTP " + response.statusCode());
            }
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo serializar el email para Brevo", error);
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo conectar con Brevo", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La solicitud a Brevo fue interrumpida", error);
        }
    }
}
