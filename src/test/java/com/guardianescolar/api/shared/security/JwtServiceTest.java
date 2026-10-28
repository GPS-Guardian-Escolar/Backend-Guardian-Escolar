package com.guardianescolar.api.shared.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.junit.jupiter.api.Test;

import com.guardianescolar.api.modules.security.domain.User;
import java.util.UUID;

class JwtServiceTest {

    @Test
    void generateTokenShouldSupportSecretsThatAreNotBase64Encoded() {
        JwtService jwtService = new JwtService(
                "guardian-escolar-desarrollo-cambiar-por-secreto-de-32-bytes-minimo",
                30L);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("demo@guardian.com");
        user.setFullName("User Demo");

        String token = assertDoesNotThrow(() -> jwtService.generateToken(user));

        assertFalse(token.isBlank());
    }

    @Test
    void rejectMissingOrShortSecretsInsteadOfPaddingThem() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 30L));
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 30L));
    }

    @Test
    void springContextMustFailWhenJwtSecretIsMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtServiceConfiguration.class)
                .withPropertyValues(
                        "guardian.security.jwt.secret=",
                        "guardian.security.jwt.expiration-minutes=30")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalStateException.class));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtServiceConfiguration {
        @Bean
        JwtService jwtService(
                @Value("${guardian.security.jwt.secret}") String secret,
                @Value("${guardian.security.jwt.expiration-minutes}") long expirationMinutes) {
            return new JwtService(secret, expirationMinutes);
        }
    }
}
