package com.guardianescolar.api.modules.auth.dto;

import com.guardianescolar.api.modules.security.domain.TwoFactorMethods;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 60) @Pattern(regexp = "^(?!.*(.)\\1{3})[\\p{L}\\p{M}]+(?:[ '\\u2019-][\\p{L}\\p{M}]+)*$", message = "El nombre solo puede contener letras") String fullName,
            @NotBlank @Email @Size(max = 100) String email,
            @Pattern(regexp = "^$|^(?!(\\d)\\1{9}$)3\\d{9}$", message = "El celular debe tener 10 dígitos y comenzar por 3") String phone,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotNull @AssertTrue(message = "Debe aceptar los términos y condiciones") Boolean termsAccepted,
            @Size(max = 20) String termsVersion,
            @Size(max = 500) String returnUrl) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @Size(max = 120) String deviceIdentifier,
            @Size(max = 120) String deviceName,
            @Size(max = 30) String platform,
            @Size(max = 500) String returnUrl) {
    }

    public record PasswordRecoveryRequest(
            @NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 60) @Pattern(regexp = "^(?!.*(.)\\1{3})[\\p{L}\\p{M}]+(?:[ '\\u2019-][\\p{L}\\p{M}]+)*$", message = "El nombre solo puede contener letras") String fullName,
            @Pattern(regexp = "^$|^(?!(\\d)\\1{9}$)3\\d{9}$", message = "El celular debe tener 10 dígitos y comenzar por 3") String phone) {
    }

    public record ChangeEmailRequest(
            @NotBlank String currentPassword,
            @NotBlank @Email @Size(max = 100) String newEmail) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {
    }

    public record RequestTwoFactorRequest(
            @NotBlank @Pattern(regexp = TwoFactorMethods.REGEX, message = "método debe ser EMAIL o SMS") String method) {
    }

    public record ResendTwoFactorRequest(
            @NotBlank String token) {
    }

    public record VerifyTwoFactorRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "código debe tener 6 dígitos") String code,
            @Pattern(regexp = TwoFactorMethods.REGEX, message = "método debe ser EMAIL o SMS") String method,
            @NotBlank @Size(max = 120) String deviceIdentifier,
            @NotBlank @Size(max = 120) String deviceName,
            @NotBlank @Size(max = 30) String platform,
            @Size(max = 500) String returnUrl) {
    }

    public record DisableTwoFactorRequest(
            @NotBlank String currentPassword) {
    }

    public record TwoFactorChallengeResponse(
            String message,
            String twoFactorToken,
            String twoFactorMethod) {
    }

    public record MessageResponse(
            String message) {
    }

    public record UserResponse(
            UUID id,
            String fullName,
            String phone,
            String email,
            Boolean emailVerified,
            Boolean twoFactorEnabled,
            String twoFactorMethod,
            Boolean termsAccepted,
            String termsVersion,
            Set<String> roles) {
    }

    public record AuthResponse(
            String token,
            long expiresInMinutes,
            UserResponse user,
            Boolean requiresTwoFactor,
            String twoFactorToken,
            String twoFactorMethod) {
    }
}

