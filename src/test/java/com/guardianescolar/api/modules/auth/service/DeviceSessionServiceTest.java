package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.security.domain.SessionStatus;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.domain.UserSession;
import com.guardianescolar.api.modules.security.repository.UserSessionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceSessionServiceTest {

    private UserSessionRepository repository;
    private EmailService emailService;
    private AuthValidationService validationService;
    private DeviceSessionService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserSessionRepository.class);
        emailService = mock(EmailService.class);
        validationService = mock(AuthValidationService.class);
        service = new DeviceSessionService(repository, emailService, validationService);
    }

    @Test
    void newDeviceSendsConfirmationAndDoesNotAuthorizeLogin() {
        User user = user();
        UserSession knownDevice = activeSession("known-device");
        AuthDtos.LoginRequest request = loginRequest("new-device");

        when(validationService.normalizeOptionalText("new-device")).thenReturn("new-device");
        when(validationService.normalizeOptionalText("Chrome en Android")).thenReturn("Chrome en Android");
        when(validationService.normalizeOptionalText("ANDROID")).thenReturn("ANDROID");
        when(validationService.normalizeReturnUrl(request.returnUrl())).thenReturn(request.returnUrl());
        when(repository.existsByUserIdAndDeviceIdentifierAndSessionStatusAndEmailConfirmedTrue(
                user.getId(), "new-device", SessionStatus.ACTIVE)).thenReturn(false);
        when(repository.findByUserIdAndSessionStatusAndEmailConfirmedTrueAndDeviceIdentifierIsNotNull(
                user.getId(), SessionStatus.ACTIVE)).thenReturn(List.of(knownDevice));
        when(repository.findFirstByUserIdAndDeviceIdentifierAndEmailConfirmedFalseOrderByCreatedAtDesc(
                user.getId(), "new-device")).thenReturn(Optional.empty());

        DeviceConfirmationRequiredException exception = assertThrows(DeviceConfirmationRequiredException.class,
                () -> service.validateDeviceLogin(user, request));

        assertTrue(exception.getMessage().contains("Revisa tu email"));
        verify(emailService).enviarConfirmacionNuevoDispositivo(
                eq(user.getEmail()), any(), eq("Chrome en Android"), eq("ANDROID"), eq(request.returnUrl()));
        verify(repository).save(any(UserSession.class));
    }

    @Test
    void confirmedTokenIsSingleUseAndActivatesPendingSession() {
        UserSession pending = new UserSession();
        pending.setDeviceConfirmationToken("one-time-token");
        pending.setDeviceConfirmationExpiresAt(OffsetDateTime.now().plusMinutes(5));
        pending.setEmailConfirmed(false);
        pending.setSessionStatus(SessionStatus.EXPIRED);
        when(repository.findFirstByDeviceConfirmationToken("one-time-token")).thenReturn(Optional.of(pending));

        var response = service.confirmLogin("one-time-token");

        assertTrue(pending.getEmailConfirmed());
        assertEquals(SessionStatus.ACTIVE, pending.getSessionStatus());
        assertEquals(null, pending.getDeviceConfirmationToken());
        assertEquals(null, pending.getDeviceConfirmationExpiresAt());
        assertTrue(response.message().contains("Dispositivo confirmado"));
        verify(repository).save(pending);
    }

    @Test
    void expiredTokenCannotConfirmDevice() {
        UserSession pending = new UserSession();
        pending.setDeviceConfirmationToken("expired-token");
        pending.setDeviceConfirmationExpiresAt(OffsetDateTime.now().minusMinutes(1));
        pending.setEmailConfirmed(false);
        when(repository.findFirstByDeviceConfirmationToken("expired-token")).thenReturn(Optional.of(pending));

        assertThrows(IllegalArgumentException.class, () -> service.confirmLogin("expired-token"));
        assertFalse(pending.getEmailConfirmed());
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@example.com");
        return user;
    }

    private UserSession activeSession(String deviceIdentifier) {
        UserSession session = new UserSession();
        session.setDeviceIdentifier(deviceIdentifier);
        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setEmailConfirmed(true);
        return session;
    }

    private AuthDtos.LoginRequest loginRequest(String deviceIdentifier) {
        return new AuthDtos.LoginRequest(
                "owner@example.com",
                "Password1!",
                deviceIdentifier,
                "Chrome en Android",
                "ANDROID",
                "https://frontend.example.com/login");
    }
}
