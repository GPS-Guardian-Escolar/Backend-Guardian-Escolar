package com.guardianescolar.api.shared.config;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Service health endpoints")
public class HealthController {

    @Operation(summary = "Consultar estado de la API")
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "status", "ok",
                "message", "API GPS Guardian Escolar funcionando",
                "health", "/api/health");
    }

    @Operation(summary = "Consultar healthcheck de la API")
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
