package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.notifications.repository.NotificationSettingsRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.OffsetDateTime;

class AuthProfileAndRecoveryTest {
    private UserRepository userRepository;
    private EmailService emailService;
    private CurrentUserService currentUserService;
    private AuthValidationService validationService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailService = mock(EmailService.class);
        currentUserService = mock(CurrentUserService.class);
        validationService = mock(AuthValidationService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = new AuthService(userRepository, mock(NotificationSettingsRepository.class),
                passwordEncoder, mock(JwtService.class), mock(AuthenticationManager.class),
                emailService, currentUserService, validationService, mock(AuthUserMapper.class),
                mock(RoleProvisioningService.class), mock(DeviceSessionService.class),
                mock(TwoFactorService.class));
    }

    @Test
    void passwordResetRejectsTheCurrentPassword() {
        User user = new User();
        user.setPasswordHash("encoded-current");
        user.setPasswordResetExpiresAt(OffsetDateTime.now().plusMinutes(5));
        when(userRepository.findByPasswordResetToken("valid-token")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("SamePassword1!", "encoded-current")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.restablecerPassword(new AuthDtos.ResetPasswordRequest("valid-token", "SamePassword1!")));
        verify(userRepository, never()).save(user);
    }

    @Test
    void recoveryDoesNotClaimDeliveryWhenEmailIsDisabled() {
        assertThrows(EmailUnavailableException.class,
                () -> authService.solicitarRestablecimiento(new AuthDtos.PasswordRecoveryRequest("ana@example.com")));
        verify(userRepository, never()).findByEmailIgnoreCase("ana@example.com");
    }

    @Test
    void emailChangeLeavesAccountIntactWhenDeliveryIsDisabled() {
        User user = new User();
        user.setEmail("ana@example.com");
        user.setEmailVerified(true);
        when(currentUserService.currentUser()).thenReturn(user);
        when(validationService.normalizeEmail("nuevo@example.com")).thenReturn("nuevo@example.com");

        assertThrows(EmailUnavailableException.class,
                () -> authService.cambiarEmail(new AuthDtos.ChangeEmailRequest("clave", "nuevo@example.com")));
        assertEquals("ana@example.com", user.getEmail());
        assertEquals(true, user.getEmailVerified());
        verify(userRepository, never()).save(user);
    }

    @Test
    void registrationDoesNotCreateAccountWhenEmailIsDisabled() {
        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                "Ana Gomez", "ana@example.com", "3001234567", "Password1!", true, null, null);
        when(validationService.normalizeEmail(request.email())).thenReturn(request.email());
        when(validationService.normalizePhone(request.phone())).thenReturn(request.phone());

        assertThrows(EmailUnavailableException.class, () -> authService.register(request));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void profileUpdateUsesColombianPhoneValidationAndNormalization() {
        User user = new User();
        when(currentUserService.currentUser()).thenReturn(user);
        when(validationService.normalizePhone("+57 300 123 4567")).thenReturn("3001234567");
        when(userRepository.save(user)).thenReturn(user);

        authService.actualizarPerfil(new AuthDtos.UpdateProfileRequest("Ana Gomez", "+57 300 123 4567"));

        assertEquals("3001234567", user.getPhone());
        verify(validationService).normalizePhone("+57 300 123 4567");
    }
}
