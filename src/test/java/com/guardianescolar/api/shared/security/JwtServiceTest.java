package com.guardianescolar.api.shared.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
