package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class ResendEmailSenderTest {

    @Test
    void sendsResendPayloadAndAuthorizationHeader() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> handle(exchange, authorization, body));
        server.start();

        try {
            ResendEmailSender sender = new ResendEmailSender(
                    "re_test_key",
                    URI.create("http://localhost:" + server.getAddress().getPort()),
                    HttpClient.newHttpClient(),
                    new ObjectMapper());

            sender.send("recipient@example.com", "onboarding@resend.dev", "Subject", "Plain", "<p>HTML</p>");

            JsonNode payload = new ObjectMapper().readTree(body.get());
            assertEquals("Bearer re_test_key", authorization.get());
            assertEquals("onboarding@resend.dev", payload.get("from").asText());
            assertEquals("recipient@example.com", payload.get("to").get(0).asText());
            assertEquals("Subject", payload.get("subject").asText());
            assertEquals("Plain", payload.get("text").asText());
            assertTrue(payload.get("html").asText().contains("HTML"));
        } finally {
            server.stop(0);
        }
    }

    private static void handle(
            HttpExchange exchange,
            AtomicReference<String> authorization,
            AtomicReference<String> body) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{\"id\":\"email-id\"}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
