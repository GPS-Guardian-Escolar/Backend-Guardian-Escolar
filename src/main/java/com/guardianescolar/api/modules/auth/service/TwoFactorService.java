package com.guardianescolar.api.modules.auth.service;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.security.domain.TwoFactorMethods;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final int TWO_FACTOR_EXPIRATION_MINUTES = 10;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final int MAX_CHALLENGE_REQUESTS_PER_USER = 5;
    private static final int MAX_CHALLENGE_REQUESTS_PER_IP = 20;
    private static final int MAX_VERIFY_ATTEMPTS_PER_IP = 50;
    private static final Duration CHALLENGE_REQUEST_WINDOW = Duration.ofHours(1);
    private static final Duration CODE_ATTEMPT_WINDOW = Duration.ofMinutes(TWO_FACTOR_EXPIRATION_MINUTES);
    private static final Duration VERIFY_IP_WINDOW = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuthValidationService validationService;
    private final AuthAttemptLimiter attemptLimiter;

    public void prepareAndSend(User user, String method) {
        prepareCode(user, method);
        userRepository.save(user);
        sendCode(user);
    }

    public AuthDtos.TwoFactorChallengeResponse request(User user, String method) {
        recordChallengeRequest(user);
        String normalizedMethod = validationService.normalizeTwoFactorMethod(method);
        validationService.validateTwoFactorMethodAvailable(user, normalizedMethod);
        prepareAndSend(user, normalizedMethod);
        return challengeResponse("Codigo de verificacion enviado", user);
    }

    public AuthDtos.TwoFactorChallengeResponse resend(AuthDtos.ResendTwoFactorRequest request) {
        limitVerifyByIp();
        User user = userRepository.findByTwoFactorToken(request.token())
                .orElseThrow(() -> {
                    recordVerifyFailureByIp();
                    return new IllegalArgumentException("Token de verificacion 2FA invalido");
                });
        recordChallengeRequest(user);
        String method = validationService.normalizeTwoFactorMethod(user.getTwoFactorMethod());
        validationService.validateTwoFactorMethodAvailable(user, method);
        prepareAndSend(user, method);
        return challengeResponse("Codigo de verificacion reenviado", user);
    }

    public User verify(AuthDtos.VerifyTwoFactorRequest request) {
        limitVerifyByIp();
        User user = userRepository.findByTwoFactorToken(request.token())
                .orElseThrow(() -> {
                    recordVerifyFailureByIp();
                    return new IllegalArgumentException("Token de verificacion 2FA invalido");
                });
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
    }

    private void sendCode(User user) {
        if (!emailService.enviarCodeDosFactores(user.getEmail(), user.getTwoFactorCode())) {
            throw new EmailUnavailableException();
        }
    }

    private void validateCode(User user, String code) {
        String token = user.getTwoFactorToken();
        attemptLimiter.assertAllowed("2fa-code", token, MAX_CODE_ATTEMPTS, CODE_ATTEMPT_WINDOW);
        if (user.getTwoFactorExpiresAt() == null || user.getTwoFactorExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Codigo de verificacion expirado");
        }
        if (user.getTwoFactorCode() == null || !user.getTwoFactorCode().equals(code)) {
            attemptLimiter.record("2fa-code", token, MAX_CODE_ATTEMPTS, CODE_ATTEMPT_WINDOW);
            throw new IllegalArgumentException("Codigo de verificacion incorrecto");
        }
        attemptLimiter.clear("2fa-code", token);
    }

    private void recordChallengeRequest(User user) {
        String userId = user.getId() == null ? user.getEmail() : user.getId().toString();
        String ip = attemptLimiter.clientIp();
        int userRequests = attemptLimiter.record(
                "2fa-challenge-user", userId, MAX_CHALLENGE_REQUESTS_PER_USER, CHALLENGE_REQUEST_WINDOW);
        int ipRequests = attemptLimiter.record(
                "2fa-challenge-ip", ip, MAX_CHALLENGE_REQUESTS_PER_IP, CHALLENGE_REQUEST_WINDOW);
        if (userRequests > MAX_CHALLENGE_REQUESTS_PER_USER || ipRequests > MAX_CHALLENGE_REQUESTS_PER_IP) {
            throw new AuthRateLimitException();
        }
    }

    private void limitVerifyByIp() {
        attemptLimiter.assertAllowed(
                "2fa-verify-ip", attemptLimiter.clientIp(), MAX_VERIFY_ATTEMPTS_PER_IP, VERIFY_IP_WINDOW);
    }

    private void recordVerifyFailureByIp() {
        attemptLimiter.record(
                "2fa-verify-ip", attemptLimiter.clientIp(), MAX_VERIFY_ATTEMPTS_PER_IP, VERIFY_IP_WINDOW);
    }

    private void clearCode(User user) {
        if (user.getTwoFactorToken() != null) {
            attemptLimiter.clear("2fa-code", user.getTwoFactorToken());
        }
        user.setTwoFactorCode(null);
        user.setTwoFactorToken(null);
        user.setTwoFactorExpiresAt(null);
    }

    private AuthDtos.TwoFactorChallengeResponse challengeResponse(String message, User user) {
        return new AuthDtos.TwoFactorChallengeResponse(
                message,
                user.getTwoFactorToken(),
                user.getTwoFactorMethod());
    }
}
