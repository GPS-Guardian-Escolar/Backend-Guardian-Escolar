package com.guardianescolar.api.modules.auth.service;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.notifications.service.SmsService;
import com.guardianescolar.api.modules.security.domain.TwoFactorMethods;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final int TWO_FACTOR_EXPIRATION_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final AuthValidationService validationService;

    public void prepareAndSend(User user, String method) {
        prepareCode(user, method);
        userRepository.save(user);
        sendCode(user, method);
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
    }

    private void sendCode(User user, String method) {
        if (TwoFactorMethods.SMS.equalsIgnoreCase(method)) {
            if (!smsService.send(user.getPhone(), "Your GPS Guardian Escolar code is: " + user.getTwoFactorCode())) {
                throw new IllegalStateException("Could not send the SMS code");
            }
            return;
        }
        if (!emailService.enviarCodeDosFactores(user.getEmail(), user.getTwoFactorCode())) {
            throw new IllegalStateException("Email delivery is not configured");
        }
    }

    private void validateCode(User user, String code) {
        if (user.getTwoFactorExpiresAt() == null || user.getTwoFactorExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Código de verificación expirado");
        }
        if (user.getTwoFactorCode() == null || !user.getTwoFactorCode().equals(code)) {
            throw new IllegalArgumentException("Código de verificación incorrecto");
        }
    }

    private void clearCode(User user) {
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

