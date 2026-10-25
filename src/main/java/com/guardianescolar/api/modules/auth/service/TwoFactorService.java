package com.guardianescolar.api.modules.auth.service;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.security.domain.TwoFactorMethods;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final int TWO_FACTOR_EXPIRATION_MINUTES = 10;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, AttemptWindow> ATTEMPTS = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuthValidationService validationService;

    public void prepareAndSend(User user, String method) {
        prepareCode(user, method);
        userRepository.save(user);
        sendCode(user);
    }

    public AuthDtos.TwoFactorChallengeResponse request(User user, String method) {
        String normalizedMethod = validationService.normalizeTwoFactorMethod(method);
        validationService.validateTwoFactorMethodAvailable(user, normalizedMethod);
        prepareAndSend(user, normalizedMethod);
        return challengeResponse("Código de verificación enviado", user);
    }

    public AuthDtos.TwoFactorChallengeResponse resend(AuthDtos.ResendTwoFactorRequest request) {
        User user = userRepository.findByTwoFactorToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Token de verificación 2FA inválido"));
        String method = validationService.normalizeTwoFactorMethod(user.getTwoFactorMethod());
        validationService.validateTwoFactorMethodAvailable(user, method);
        prepareAndSend(user, method);
        return challengeResponse("Código de verificación reenviado", user);
    }

    public User verify(AuthDtos.VerifyTwoFactorRequest request) {
        User user = userRepository.findByTwoFactorToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Token de verificación 2FA inválido"));
        validateCode(user, request.code());
        if (request.method() != null && !request.method().isBlank()) {
            user.setTwoFactorMethod(validationService.normalizeTwoFactorMethod(request.method()));
        }
        user.setTwoFactorEnabled(true);
        clearCode(user);
        return userRepository.save(user);
    }

    public User disable(User user) {
        user.setTwoFactorEnabled(false);
        user.setTwoFactorMethod(TwoFactorMethods.EMAIL);
        clearCode(user);
        return userRepository.save(user);
    }

    private void prepareCode(User user, String method) {
        user.setTwoFactorMethod(validationService.normalizeTwoFactorMethod(method));
        user.setTwoFactorCode(String.format("%06d", RANDOM.nextInt(1_000_000)));
        user.setTwoFactorToken(UUID.randomUUID().toString());
        user.setTwoFactorExpiresAt(OffsetDateTime.now().plusMinutes(TWO_FACTOR_EXPIRATION_MINUTES));
        ATTEMPTS.remove(user.getTwoFactorToken());
    }

    private void sendCode(User user) {
        if (!emailService.enviarCodeDosFactores(user.getEmail(), user.getTwoFactorCode())) {
            throw new EmailUnavailableException();
        }
    }

    private void validateCode(User user, String code) {
        if (user.getTwoFactorExpiresAt() == null || user.getTwoFactorExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Código de verificación expirado");
        }
        String token = user.getTwoFactorToken();
        AttemptWindow attempts = ATTEMPTS.computeIfAbsent(token, ignored -> new AttemptWindow());
        if (attempts.isBlocked()) {
            throw new IllegalArgumentException("Demasiados intentos. Solicita un nuevo código");
        }
        if (user.getTwoFactorCode() == null || !user.getTwoFactorCode().equals(code)) {
            if (attempts.failures.incrementAndGet() >= MAX_CODE_ATTEMPTS) {
                attempts.blockedUntil = OffsetDateTime.now().plusMinutes(TWO_FACTOR_EXPIRATION_MINUTES);
            }
            throw new IllegalArgumentException("Código de verificación incorrecto");
        }
        ATTEMPTS.remove(token);
    }

    private void clearCode(User user) {
        user.setTwoFactorCode(null);
        user.setTwoFactorToken(null);
        user.setTwoFactorExpiresAt(null);
    }

    private static final class AttemptWindow {
        private final AtomicInteger failures = new AtomicInteger();
        private volatile OffsetDateTime blockedUntil;

        private boolean isBlocked() {
            return blockedUntil != null && blockedUntil.isAfter(OffsetDateTime.now());
        }
    }

    private AuthDtos.TwoFactorChallengeResponse challengeResponse(String message, User user) {
        return new AuthDtos.TwoFactorChallengeResponse(
                message,
                user.getTwoFactorToken(),
                user.getTwoFactorMethod());
    }
}

