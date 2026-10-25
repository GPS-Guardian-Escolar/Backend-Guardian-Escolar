package com.guardianescolar.api.modules.auth.service;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.security.domain.SessionStatus;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.domain.UserSession;
import com.guardianescolar.api.modules.security.repository.UserSessionRepository;
import java.time.OffsetDateTime;
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
        sessionRequest.setToken("PENDING_DEVICE_CONFIRMATION:" + confirmationToken);
        sessionRequest.setDeviceIdentifier(deviceIdentifier);
        sessionRequest.setDeviceName(validationService.normalizeOptionalText(request.deviceName()));
        sessionRequest.setPlatform(validationService.normalizeOptionalText(request.platform()));
        sessionRequest.setDeviceConfirmationReturnUrl(validationService.normalizeReturnUrl(request.returnUrl()));
        sessionRequest.setSessionStatus(SessionStatus.EXPIRED);
        sessionRequest.setEmailConfirmed(false);
        sessionRequest.setDeviceConfirmationToken(confirmationToken);
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
        session.setToken(token);
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

    public AuthDtos.MessageResponse confirmLogin(String token) {
        UserSession session = userSessionRepository.findFirstByDeviceConfirmationToken(token)
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
}
