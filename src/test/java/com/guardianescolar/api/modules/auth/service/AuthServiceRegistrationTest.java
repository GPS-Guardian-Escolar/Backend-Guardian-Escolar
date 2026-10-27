package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.notifications.repository.NotificationSettingsRepository;
import com.guardianescolar.api.modules.security.domain.Role;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

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
    @Mock AuthAttemptLimiter authAttemptLimiter;

    private AuthService authService;
    private AuthDtos.RegisterRequest request;
    private Role parentRole;

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
                twoFactorService,
                authAttemptLimiter);
        request = new AuthDtos.RegisterRequest(
                "Nicolas Ruiz",
                "nicolas@example.com",
                "+573001234567",
                "Password1!",
                true,
                null,
                "https://frontend.example.com");
        parentRole = mock(Role.class);
        when(validationService.normalizeEmail(request.email())).thenReturn(request.email());
        when(validationService.normalizePhone(request.phone())).thenReturn(request.phone());
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(roleProvisioningService.parentRole()).thenReturn(parentRole);
        when(emailService.isAvailable()).thenReturn(true);
    }

    @Test
    void registersUserAndRequestsVerificationEmail() {
        when(validationService.normalizeReturnUrl(request.returnUrl())).thenReturn(request.returnUrl());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(null);

        AuthDtos.AuthResponse response = authService.register(request);

        assertEquals(false, response.requiresTwoFactor());
        verify(userRepository).save(any(User.class));
        verify(notificationSettingsRepository).save(any());
        verify(emailService).enviarVerificacionEmail(eq(request.email()), any(), any());
    }

    @Test
    void propagatesProviderFailureSoTransactionalRegistrationCanRollback() {
        when(validationService.normalizeReturnUrl(request.returnUrl())).thenReturn(request.returnUrl());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EmailDeliveryException providerFailure = new EmailDeliveryException(
                "brevo", 400, "Brevo rechazo el email con HTTP 400", null);
        doThrow(providerFailure).when(emailService)
                .enviarVerificacionEmail(any(), any(), any());

        EmailDeliveryException thrown = assertThrows(EmailDeliveryException.class,
                () -> authService.register(request));

        assertEquals(providerFailure, thrown);
        verify(userRepository).save(any(User.class));
        verify(notificationSettingsRepository).save(any());
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    @Test
    void propagatesPersistenceFailureBeforeEmailIsRequested() {
        doThrow(new DataIntegrityViolationException("duplicate email"))
                .when(userRepository).save(any(User.class));

        assertThrows(DataIntegrityViolationException.class, () -> authService.register(request));

        verify(emailService, never()).enviarVerificacionEmail(any(), any(), any());
        verify(notificationSettingsRepository, never()).save(any());
    }

}
