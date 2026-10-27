package com.guardianescolar.api.modules.auth.controller;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.auth.service.AuthService;
import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.auth.dto.AuthDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, verification and account security")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Registrar una cuenta")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Iniciar sesión")
    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Consultar el usuario autenticado")
    @GetMapping("/me")
    public AuthDtos.UserResponse me() {
        return authService.toUserResponse(currentUserService.currentUser());
    }

    @Operation(summary = "Verificar correo electrónico")
    @GetMapping("/verify-email")
    public AuthDtos.MessageResponse verificarEmail(@RequestParam String token) {
        return authService.verificarEmail(token.trim());
    }

    @Operation(summary = "Confirmar inicio de sesión en dispositivo")
    @GetMapping("/confirm-login")
    public AuthDtos.MessageResponse confirmarInicioSesion(@RequestParam String token) {
        return authService.confirmarInicioSesion(token.trim());
    }

    @Operation(summary = "Solicitar restablecimiento de contraseña")
    @PostMapping("/password/forgot")
    public AuthDtos.MessageResponse solicitarRestablecimiento(
            @Valid @RequestBody AuthDtos.PasswordRecoveryRequest request) {
        return authService.solicitarRestablecimiento(request);
    }

    @Operation(summary = "Restablecer contraseña")
    @PostMapping("/password/reset")
    public AuthDtos.MessageResponse restablecerPassword(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        return authService.restablecerPassword(request);
    }

    @Operation(summary = "Actualizar perfil")
    @PutMapping("/profile")
    public AuthDtos.UserResponse actualizarPerfil(
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        return authService.actualizarPerfil(request);
    }

    @Operation(summary = "Cambiar correo electrónico")
    @PutMapping("/email")
    public AuthDtos.UserResponse cambiarEmail(
            @Valid @RequestBody AuthDtos.ChangeEmailRequest request) {
        return authService.cambiarEmail(request);
    }

    @Operation(summary = "Cambiar contraseña")
    @PutMapping("/password")
    public AuthDtos.MessageResponse cambiarPassword(
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        return authService.cambiarPassword(request);
    }

    @Operation(summary = "Solicitar código de doble factor")
    @PostMapping("/2fa/request")
    public AuthDtos.TwoFactorChallengeResponse solicitarDosFactores(
            @Valid @RequestBody AuthDtos.RequestTwoFactorRequest request) {
        return authService.solicitarDosFactores(request);
    }

    @Operation(summary = "Reenviar código de doble factor")
    @PostMapping("/2fa/resend")
    public AuthDtos.TwoFactorChallengeResponse reenviarDosFactores(
            @Valid @RequestBody AuthDtos.ResendTwoFactorRequest request) {
        return authService.reenviarDosFactores(request);
    }

    @Operation(summary = "Verificar código de doble factor")
    @PostMapping("/2fa/verify")
    public AuthDtos.AuthResponse verificarDosFactores(
            @Valid @RequestBody AuthDtos.VerifyTwoFactorRequest request) {
        return authService.verificarDosFactores(request);
    }

    @Operation(summary = "Desactivar doble factor")
    @PostMapping("/2fa/disable")
    public AuthDtos.UserResponse desactivarDosFactores(
            @Valid @RequestBody AuthDtos.DisableTwoFactorRequest request) {
        return authService.desactivarDosFactores(request);
    }
}
