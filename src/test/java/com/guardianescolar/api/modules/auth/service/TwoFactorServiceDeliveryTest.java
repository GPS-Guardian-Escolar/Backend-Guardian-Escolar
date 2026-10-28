package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TwoFactorServiceDeliveryTest {
    @Test
    void emailChallengeDoesNotClaimCodeWasSentWhenDeliveryIsDisabled() {
        UserRepository users = mock(UserRepository.class);
        EmailService email = mock(EmailService.class);
        AuthValidationService validation = mock(AuthValidationService.class);
        AuthAttemptLimiter limiter = mockLimiter();
        TwoFactorService service = new TwoFactorService(users, email, validation, limiter);
        User user = user();

        when(validation.normalizeTwoFactorMethod("EMAIL")).thenReturn("EMAIL");

        assertThrows(EmailUnavailableException.class, () -> service.request(user, "EMAIL"));

        verify(email).enviarCodeDosFactores(org.mockito.ArgumentMatchers.eq("ana@example.com"), any());
    }

    @Test
    void blocksTheSixthWrongTwoFactorCodeAttempt() {
        UserRepository users = mock(UserRepository.class);
        EmailService email = mock(EmailService.class);
        AuthValidationService validation = mock(AuthValidationService.class);
        AuthAttemptLimiter limiter = mockLimiter();
        AtomicInteger codeAttempts = new AtomicInteger();
        when(limiter.record(anyString(), anyString(), anyInt(), any(Duration.class)))
                .thenAnswer(invocation -> "2fa-code".equals(invocation.getArgument(0))
                        ? codeAttempts.incrementAndGet()
                        : 1);
        doAnswer(invocation -> {
            if ("2fa-code".equals(invocation.getArgument(0)) && codeAttempts.get() >= 5) {
                throw new AuthRateLimitException();
            }
            return null;
        }).when(limiter).assertAllowed(anyString(), anyString(), anyInt(), any(Duration.class));

        TwoFactorService service = new TwoFactorService(users, email, validation, limiter);
        User user = user();
        when(validation.normalizeTwoFactorMethod("EMAIL")).thenReturn("EMAIL");
        when(email.enviarCodeDosFactores(org.mockito.ArgumentMatchers.eq("ana@example.com"), any())).thenReturn(true);
        when(users.findByTwoFactorToken(any())).thenReturn(Optional.of(user));

        service.request(user, "EMAIL");
        String wrongCode = "000000".equals(user.getTwoFactorCode()) ? "000001" : "000000";

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.verify(new AuthDtos.VerifyTwoFactorRequest(
                            user.getTwoFactorToken(), wrongCode, "EMAIL", "device", "Browser", "WEB", null)));
        }

        assertThrows(AuthRateLimitException.class,
                () -> service.verify(new AuthDtos.VerifyTwoFactorRequest(
                        user.getTwoFactorToken(), user.getTwoFactorCode(), "EMAIL", "device", "Browser", "WEB", null)));
    }

    private static User user() {
        User user = new User();
        user.setEmail("ana@example.com");
        return user;
    }

    private static AuthAttemptLimiter mockLimiter() {
        AuthAttemptLimiter limiter = mock(AuthAttemptLimiter.class);
        when(limiter.clientIp()).thenReturn("198.51.100.10");
        when(limiter.record(anyString(), anyString(), anyInt(), any(Duration.class))).thenReturn(1);
        return limiter;
    }
}