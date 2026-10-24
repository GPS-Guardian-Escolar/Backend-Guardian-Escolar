package com.guardianescolar.api.modules.auth.service;

import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import com.guardianescolar.api.modules.notifications.domain.NotificationSettings;
import com.guardianescolar.api.modules.notifications.repository.NotificationSettingsRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.shared.security.JwtService;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TERMS_VERSION = "2026-08-20";

    private final UserRepository userRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;
    private final AuthValidationService validationService;
    private final AuthUserMapper userMapper;
    private final RoleProvisioningService roleProvisioningService;
    private final DeviceSessionService deviceSessionService;
    private final TwoFactorService twoFactorService;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        validationService.validatePasswordPolicy(request.password());
        String email = validationService.normalizeEmail(request.email());
        validationService.validateRegistrableEmail(email);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con este correo electrónico");
        }

        String phone = validationService.normalizePhone(request.phone());
        validationService.validatePhoneAvailable(phone);
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new IllegalArgumentException("Debe aceptar los términos y condiciones");
        }
        if (!emailService.isAvailable()) {
            throw new EmailUnavailableException();
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setIsActive(false);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(generateToken());
        user.setEmailVerificationExpiresAt(OffsetDateTime.now().plusHours(24));
        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(OffsetDateTime.now());
        user.setTermsVersion(normalizeTermsVersion(request.termsVersion()));
        user.getRoles().add(roleProvisioningService.parentRole());
        user = userRepository.save(user);

        NotificationSettings settings = new NotificationSettings();
        settings.setUser(user);
        notificationSettingsRepository.save(settings);

        emailService.enviarVerificacionEmail(
                user.getEmail(),
                user.getEmailVerificationToken(),
                validationService.normalizeReturnUrl(request.returnUrl()));
        return authResponse("", 0, userMapper.toUserResponse(user), false, null, null);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = validationService.normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        validateLoginUser(user);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        // The device must be approved before revealing or issuing a 2FA challenge.
        deviceSessionService.validateDeviceLogin(user, request);

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            twoFactorService.prepareAndSend(user, user.getTwoFactorMethod());
            return authResponse("", 0, userMapper.toUserResponse(user), true, user.getTwoFactorToken(), user.getTwoFactorMethod());
        }

        String token = jwtService.generateToken(user);
        deviceSessionService.saveSession(user, token, request);
        return authResponse(token, jwtService.getExpirationMinutes(), userMapper.toUserResponse(user), false, null, null);
    }

    @Transactional
    public AuthDtos.MessageResponse verificarEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de verificación inválido"));
        validateEmailVerificationToken(user);

        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        return new AuthDtos.MessageResponse("Email verificado correctamente");
    }

    @Transactional
    public AuthDtos.MessageResponse confirmarInicioSesion(String token) {
        return deviceSessionService.confirmLogin(token);
    }

    @Transactional
    public AuthDtos.MessageResponse solicitarRestablecimiento(AuthDtos.PasswordRecoveryRequest request) {
        if (!emailService.isAvailable()) {
            throw new EmailUnavailableException();
        }
        userRepository.findByEmailIgnoreCase(validationService.normalizeEmail(request.email()))
                .filter(User::getIsActive)
                .ifPresent(user -> {
                    user.setPasswordResetToken(generateToken());
                    user.setPasswordResetExpiresAt(OffsetDateTime.now().plusMinutes(30));
                    userRepository.save(user);
                    emailService.enviarRestablecimientoPassword(user.getEmail(), user.getPasswordResetToken());
                });
        return new AuthDtos.MessageResponse("Si el email existe, recibirá instrucciones para restablecer la contraseña");
    }

    @Transactional
    public AuthDtos.MessageResponse restablecerPassword(AuthDtos.ResetPasswordRequest request) {
        validationService.validatePasswordPolicy(request.newPassword());
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Token de restablecimiento inválido"));
        validatePasswordResetToken(user);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        return new AuthDtos.MessageResponse("Contraseña actualizada correctamente");
    }

    @Transactional
    public AuthDtos.UserResponse actualizarPerfil(AuthDtos.UpdateProfileRequest request) {
        User user = currentUserService.currentUser();
        user.setFullName(request.fullName().trim());
        user.setPhone(validationService.normalizeOptionalText(request.phone()));
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public AuthDtos.UserResponse cambiarEmail(AuthDtos.ChangeEmailRequest request) {
        User user = currentUserService.currentUser();
        validationService.validateCurrentPassword(user, request.currentPassword());
        String newEmail = validationService.normalizeEmail(request.newEmail());
        validationService.validateRegistrableEmail(newEmail);
        boolean changedEmail = !user.getEmail().equalsIgnoreCase(newEmail);
        if (changedEmail && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con este correo electrónico");
        }
        if (!changedEmail) {
            return userMapper.toUserResponse(user);
        }
        if (!emailService.isAvailable()) {
            throw new EmailUnavailableException();
        }

        user.setEmail(newEmail);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(generateToken());
        user.setEmailVerificationExpiresAt(OffsetDateTime.now().plusHours(24));
        User saved = userRepository.save(user);
        emailService.enviarVerificacionEmail(saved.getEmail(), saved.getEmailVerificationToken(), null);
        return userMapper.toUserResponse(saved);
    }

    @Transactional
    public AuthDtos.MessageResponse cambiarPassword(AuthDtos.ChangePasswordRequest request) {
        validationService.validatePasswordPolicy(request.newPassword());
        User user = currentUserService.currentUser();
        validationService.validateCurrentPassword(user, request.currentPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return new AuthDtos.MessageResponse("Contraseña actualizada correctamente");
    }

    @Transactional
    public AuthDtos.TwoFactorChallengeResponse solicitarDosFactores(AuthDtos.RequestTwoFactorRequest request) {
        return twoFactorService.request(currentUserService.currentUser(), request.method());
    }

    @Transactional
    public AuthDtos.TwoFactorChallengeResponse reenviarDosFactores(AuthDtos.ResendTwoFactorRequest request) {
        return twoFactorService.resend(request);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public AuthDtos.AuthResponse verificarDosFactores(AuthDtos.VerifyTwoFactorRequest request) {
        User user = twoFactorService.verify(request);
        AuthDtos.LoginRequest loginRequest = new AuthDtos.LoginRequest(
                user.getEmail(),
                "",
                request.deviceIdentifier(),
                request.deviceName(),
                request.platform(),
                request.returnUrl());
        deviceSessionService.validateDeviceLogin(user, loginRequest);
        String token = jwtService.generateToken(user);
        deviceSessionService.saveSession(user, token, loginRequest);
        return authResponse(token, jwtService.getExpirationMinutes(), userMapper.toUserResponse(user), false, null, null);
    }

    @Transactional
    public AuthDtos.UserResponse desactivarDosFactores(AuthDtos.DisableTwoFactorRequest request) {
        User user = currentUserService.currentUser();
        validationService.validateCurrentPassword(user, request.currentPassword());
        return userMapper.toUserResponse(twoFactorService.disable(user));
    }

    public AuthDtos.UserResponse toUserResponse(User user) {
        return userMapper.toUserResponse(user);
    }

    private void validateLoginUser(User user) {
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Debes verificar tu email electrónico antes de iniciar sesión");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
    }

    private void validateEmailVerificationToken(User user) {
        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Token de verificación expirado");
        }
    }

    private void validatePasswordResetToken(User user) {
        if (user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Token de restablecimiento expirado");
        }
    }

    private String normalizeTermsVersion(String termsVersion) {
        if (termsVersion == null || termsVersion.isBlank()) {
            return TERMS_VERSION;
        }
        return termsVersion.trim();
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private AuthDtos.AuthResponse authResponse(
            String token,
            long expiresInMinutes,
            AuthDtos.UserResponse user,
            boolean requiresTwoFactor,
            String twoFactorToken,
            String twoFactorMethod) {
        return new AuthDtos.AuthResponse(
                token,
                expiresInMinutes,
                user,
                requiresTwoFactor,
                twoFactorToken,
                twoFactorMethod);
    }
}
