package com.guardianescolar.api.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI guardianEscolarOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("GPS Guardian Escolar API")
                .version("v1")
                .description("Versioned API contract for GPS Guardian Escolar."));
    }
}
