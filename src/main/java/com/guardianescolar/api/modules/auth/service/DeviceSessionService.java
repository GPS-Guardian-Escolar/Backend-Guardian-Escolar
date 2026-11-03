package com.guardianescolar.api.modules.auth.service;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.security.domain.SessionStatus;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.domain.UserSession;
import com.guardianescolar.api.modules.security.repository.UserSessionRepository;
import com.guardianescolar.api.shared.security.TransientTokenHashService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceSessionService {

    private static final int DEVICE_CONFIRMATION_EXPIRATION_MINUTES = 15;

    private final UserSessionRepository userSessionRepository;
    private final EmailService emailService;
    private final AuthValidationService validationService;
    private final TransientTokenHashService transientTokenHashService;

    public void validateDeviceLogin(User user, AuthDtos.LoginRequest request) {
        String deviceIdentifier = validationService.normalizeOptionalText(request.deviceIdentifier());
        if (deviceIdentifier == null) {
            throw new IllegalArgumentException("El identificador del dispositivo es obligatorio");
        }
        if (isConfirmedDevice(user, deviceIdentifier) || !hasOtherConfirmedDevice(user, deviceIdentifier)) {
            return;
        }

        String confirmationToken = UUID.randomUUID().toString();
        UserSession sessionRequest = pendingSession(user, deviceIdentifier);
        sessionRequest.setUser(user);
        sessionRequest.setToken("PENDING_DEVICE_CONFIRMATION:" + transientTokenHashService.hash(confirmationToken));
        sessionRequest.setDeviceIdentifier(deviceIdentifier);
        sessionRequest.setDeviceName(validationService.normalizeOptionalText(request.deviceName()));
        sessionRequest.setPlatform(validationService.normalizeOptionalText(request.platform()));
        sessionRequest.setDeviceConfirmationReturnUrl(validationService.normalizeReturnUrl(request.returnUrl()));
        sessionRequest.setSessionStatus(SessionStatus.EXPIRED);
        sessionRequest.setEmailConfirmed(false);
        sessionRequest.setDeviceConfirmationToken(transientTokenHashService.hash(confirmationToken));
        sessionRequest.setDeviceConfirmationExpiresAt(
                OffsetDateTime.now().plusMinutes(DEVICE_CONFIRMATION_EXPIRATION_MINUTES));
        userSessionRepository.save(sessionRequest);

        emailService.enviarConfirmacionNuevoDispositivo(
                user.getEmail(),
                confirmationToken,
                sessionRequest.getDeviceName(),
                sessionRequest.getPlatform(),
                sessionRequest.getDeviceConfirmationReturnUrl());
        throw new DeviceConfirmationRequiredException(
                "Este email ya está en uso en otro dispositivo. Revisa tu email para confirmar este nuevo inicio de sesión.");
    }

    public void saveSession(User user, String token, AuthDtos.LoginRequest request) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setToken(hashSessionToken(token));
        session.setDeviceIdentifier(validationService.normalizeOptionalText(request.deviceIdentifier()));
        session.setDeviceName(validationService.normalizeOptionalText(request.deviceName()));
        session.setPlatform(validationService.normalizeOptionalText(request.platform()));
        session.setDeviceConfirmationReturnUrl(validationService.normalizeReturnUrl(request.returnUrl()));
        session.setEmailConfirmed(true);
        userSessionRepository.save(session);
    }

    public void revokeAllSessions(User user) {
        userSessionRepository.findByUserIdAndSessionStatus(user.getId(), SessionStatus.ACTIVE)
                .forEach(session -> {
                    session.setSessionStatus(SessionStatus.CLOSED);
                    session.setSessionEndedAt(OffsetDateTime.now());
                });
        userSessionRepository.flush();
    }

    public void closeSession(String token) {
        userSessionRepository.findByTokenAndSessionStatus(hashSessionToken(token), SessionStatus.ACTIVE)
                .ifPresent(session -> {
                    session.setSessionStatus(SessionStatus.CLOSED);
                    session.setSessionEndedAt(OffsetDateTime.now());
                    userSessionRepository.save(session);
                });
    }

    public AuthDtos.MessageResponse confirmLogin(String token) {
        UserSession session = userSessionRepository.findFirstByDeviceConfirmationToken(transientTokenHashService.hash(token))
                .orElseThrow(() -> new IllegalArgumentException("Token de confirmación inválido"));

        if (session.getDeviceConfirmationExpiresAt() == null
                || session.getDeviceConfirmationExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Token de confirmación expirado");
        }

        session.setEmailConfirmed(true);
        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setDeviceConfirmationToken(null);
        session.setDeviceConfirmationExpiresAt(null);
        userSessionRepository.save(session);
        return new AuthDtos.MessageResponse("Dispositivo confirmado. Ya puedes iniciar sesión desde ese equipo.");
    }

    private boolean isConfirmedDevice(User user, String deviceIdentifier) {
        return userSessionRepository.existsByUserIdAndDeviceIdentifierAndSessionStatusAndEmailConfirmedTrue(
                user.getId(),
                deviceIdentifier,
                SessionStatus.ACTIVE);
    }

    private boolean hasOtherConfirmedDevice(User user, String deviceIdentifier) {
        return userSessionRepository
                .findByUserIdAndSessionStatusAndEmailConfirmedTrueAndDeviceIdentifierIsNotNull(
                        user.getId(),
                        SessionStatus.ACTIVE)
                .stream()
                .anyMatch(session -> !deviceIdentifier.equals(session.getDeviceIdentifier()));
    }

    private UserSession pendingSession(User user, String deviceIdentifier) {
        return userSessionRepository
                .findFirstByUserIdAndDeviceIdentifierAndEmailConfirmedFalseOrderByCreatedAtDesc(
                        user.getId(),
                        deviceIdentifier)
                .orElseGet(UserSession::new);
    }

    private String hashSessionToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
