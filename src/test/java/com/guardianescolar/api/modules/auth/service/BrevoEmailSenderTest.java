package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BrevoEmailSenderTest {

    @Test
    void sendsBrevoPayloadAndApiKeyHeader() throws Exception {
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v3/smtp/email", exchange -> handle(exchange, apiKey, body));
        server.start();

        try {
            BrevoEmailSender sender = new BrevoEmailSender(
                    "brevo_test_key",
                    "GPS Guardian Escolar",
                    URI.create("http://localhost:" + server.getAddress().getPort() + "/v3/smtp/email"),
                    HttpClient.newHttpClient(),
                    new ObjectMapper());

            sender.send("recipient@example.com", "gpsguardianescolar@gmail.com", "Subject", "Plain", "<p>HTML</p>");

            JsonNode payload = new ObjectMapper().readTree(body.get());
            assertEquals("brevo_test_key", apiKey.get());
            assertEquals("gpsguardianescolar@gmail.com", payload.get("sender").get("email").asText());
            assertEquals("GPS Guardian Escolar", payload.get("sender").get("name").asText());
            assertEquals("recipient@example.com", payload.get("to").get(0).get("email").asText());
            assertEquals("Subject", payload.get("subject").asText());
            assertEquals("Plain", payload.get("textContent").asText());
            assertTrue(payload.get("htmlContent").asText().contains("HTML"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsBrevoHttpErrorsWithoutExposingResponseBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            byte[] response = "{\"message\":\"private provider details\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(429, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();

        try {
            BrevoEmailSender sender = new BrevoEmailSender(
                    "brevo_test_key",
                    "GPS Guardian Escolar",
                    URI.create("http://localhost:" + server.getAddress().getPort() + "/v3/smtp/email"),
                    HttpClient.newHttpClient(),
                    new ObjectMapper());

            EmailDeliveryException error = assertThrows(EmailDeliveryException.class, () -> sender.send(
                    "recipient@example.com", "from@example.com", "Subject", "Plain", "<p>HTML</p>"));

            assertEquals("Brevo rechazo el email con HTTP 429", error.getMessage());
            assertEquals("brevo", error.provider());
            assertEquals(429, error.providerStatus());
        } finally {
            server.stop(0);
        }
    }

    private static void handle(
            HttpExchange exchange,
            AtomicReference<String> apiKey,
            AtomicReference<String> body) throws IOException {
        apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{\"messageId\":\"test-message-id\"}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(201, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
