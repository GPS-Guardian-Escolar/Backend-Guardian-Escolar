package com.guardianescolar.api.modules.auth.controller;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.auth.service.AuthService;
import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.auth.dto.AuthDtos;
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
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me() {
        return authService.toUserResponse(currentUserService.currentUser());
    }

    @GetMapping("/verify-email")
    public AuthDtos.MessageResponse verificarEmail(@RequestParam String token) {
        return authService.verificarEmail(token.trim());
    }

    @GetMapping("/confirm-login")
    public AuthDtos.MessageResponse confirmarInicioSesion(@RequestParam String token) {
        return authService.confirmarInicioSesion(token.trim());
    }

    @PostMapping("/password/forgot")
    public AuthDtos.MessageResponse solicitarRestablecimiento(
            @Valid @RequestBody AuthDtos.PasswordRecoveryRequest request) {
        return authService.solicitarRestablecimiento(request);
    }

    @PostMapping("/password/reset")
    public AuthDtos.MessageResponse restablecerPassword(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        return authService.restablecerPassword(request);
    }

    @PutMapping("/profile")
    public AuthDtos.UserResponse actualizarPerfil(
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        return authService.actualizarPerfil(request);
    }

    @PutMapping("/email")
    public AuthDtos.UserResponse cambiarEmail(
            @Valid @RequestBody AuthDtos.ChangeEmailRequest request) {
        return authService.cambiarEmail(request);
    }

    @PutMapping("/password")
    public AuthDtos.MessageResponse cambiarPassword(
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        return authService.cambiarPassword(request);
    }

    @PostMapping("/2fa/request")
    public AuthDtos.TwoFactorChallengeResponse solicitarDosFactores(
            @Valid @RequestBody AuthDtos.RequestTwoFactorRequest request) {
        return authService.solicitarDosFactores(request);
    }

    @PostMapping("/2fa/resend")
    public AuthDtos.TwoFactorChallengeResponse reenviarDosFactores(
            @Valid @RequestBody AuthDtos.ResendTwoFactorRequest request) {
        return authService.reenviarDosFactores(request);
    }

    @PostMapping("/2fa/verify")
    public AuthDtos.AuthResponse verificarDosFactores(
            @Valid @RequestBody AuthDtos.VerifyTwoFactorRequest request) {
        return authService.verificarDosFactores(request);
    }

    @PostMapping("/2fa/disable")
    public AuthDtos.UserResponse desactivarDosFactores(
            @Valid @RequestBody AuthDtos.DisableTwoFactorRequest request) {
        return authService.desactivarDosFactores(request);
    }
}
