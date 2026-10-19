package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.notifications.repository.NotificationSettingsRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.shared.security.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTwoFactorDeviceTest {

    @Mock UserRepository userRepository;
    @Mock NotificationSettingsRepository notificationSettingsRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailService emailService;
    @Mock CurrentUserService currentUserService;
    @Mock AuthValidationService validationService;
    @Mock AuthUserMapper userMapper;
    @Mock RoleProvisioningService roleProvisioningService;
    @Mock DeviceSessionService deviceSessionService;
    @Mock TwoFactorService twoFactorService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                notificationSettingsRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                emailService,
                currentUserService,
                validationService,
                userMapper,
                roleProvisioningService,
                deviceSessionService,
                twoFactorService);
    }

    @Test
    void validatesDeviceBeforeIssuingJwtAfterTwoFactor() {
        User user = user();
        AuthDtos.VerifyTwoFactorRequest request = verifyRequest();
        when(twoFactorService.verify(request)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt");

        authService.verificarDosFactores(request);

        verify(deviceSessionService).validateDeviceLogin(user, new AuthDtos.LoginRequest(
                user.getEmail(), "", "device-2", "Chrome", "WEB", "https://frontend.example.com/login"));
        verify(jwtService).generateToken(user);
        verify(deviceSessionService).saveSession(any(), any(), any());
    }

    @Test
    void validatesDeviceBeforeSendingTwoFactorChallenge() {
        User user = user();
        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod("EMAIL");
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(
                user.getEmail(), "Password1!", "device-2", "Chrome", "WEB", "https://frontend.example.com/login");
        when(validationService.normalizeEmail(user.getEmail())).thenReturn(user.getEmail());
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(java.util.Optional.of(user));

        authService.login(request);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(deviceSessionService, twoFactorService);
        order.verify(deviceSessionService).validateDeviceLogin(user, request);
        order.verify(twoFactorService).prepareAndSend(user, "EMAIL");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void doesNotSendTwoFactorChallengeWhenDeviceNeedsConfirmation() {
        User user = user();
        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod("EMAIL");
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(
                user.getEmail(), "Password1!", "device-2", "Chrome", "WEB", "https://frontend.example.com/login");
        when(validationService.normalizeEmail(user.getEmail())).thenReturn(user.getEmail());
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.doThrow(new DeviceConfirmationRequiredException("confirm device"))
                .when(deviceSessionService).validateDeviceLogin(user, request);

        assertThrows(DeviceConfirmationRequiredException.class, () -> authService.login(request));

        verify(twoFactorService, never()).prepareAndSend(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void doesNotIssueJwtWhenNewDeviceNeedsConfirmationAfterTwoFactor() {
        User user = user();
        AuthDtos.VerifyTwoFactorRequest request = verifyRequest();
        when(twoFactorService.verify(request)).thenReturn(user);
        IllegalArgumentException pending = new IllegalArgumentException("confirmar dispositivo");
        org.mockito.Mockito.doThrow(pending)
                .when(deviceSessionService).validateDeviceLogin(any(), any());

        assertThrows(IllegalArgumentException.class, () -> authService.verificarDosFactores(request));

        verify(jwtService, never()).generateToken(any());
        verify(deviceSessionService, never()).saveSession(any(), any(), any());
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@example.com");
        return user;
    }

    private AuthDtos.VerifyTwoFactorRequest verifyRequest() {
        return new AuthDtos.VerifyTwoFactorRequest(
                "two-factor-token", "123456", "EMAIL", "device-2", "Chrome", "WEB",
                "https://frontend.example.com/login");
    }
}
