package com.guardianescolar.api.modules.auth.service;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final SmtpEmailSender smtpEmailSender;
    private final BrevoEmailSender brevoEmailSender;
    private final EmailLinkService emailLinkService;
    private final EmailTemplateService emailTemplateService;

    @Value("${guardian.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${guardian.mail.from:no-reply@guardianescolar.local}")
    private String from;

    @Value("${guardian.mail.provider:smtp}")
    private String mailProvider;

    public boolean isAvailable() {
        return mailEnabled;
    }

    public void enviarVerificacionEmail(String recipient, String token, String returnUrl) {
        String url = emailLinkService.emailVerificationUrl(token, returnUrl);
        enviar(
                recipient,
                "Verifica tu email - Guardian Escolar",
                "Confirma tu email electrónico",
                "Ya casi terminas. Verifica tu email para activar tu cuenta y empezar a usar GPS Guardian Escolar.",
                "Verificar email",
                url,
                "Si no creaste una cuenta, puedes ignorar este mensaje.");
    }

    public void enviarRestablecimientoPassword(String recipient, String token) {
        String url = emailLinkService.passwordResetUrl(token);
        enviar(
                recipient,
                "Restablece tu contraseña - Guardian Escolar",
                "Restablece tu contraseña",
                "Recibimos una solicitud para cambiar la contraseña de tu cuenta.",
                "Cambiar contraseña",
                url,
                "Si no solicitaste este cambio, ignora este email.");
    }

    public void enviarConfirmacionNuevoDispositivo(
            String recipient,
            String token,
            String deviceName,
            String platform,
            String returnUrl) {
        String url = emailLinkService.deviceConfirmationUrl(token, returnUrl);
        String dispositivo = deviceName == null || deviceName.isBlank()
                ? "un nuevo dispositivo"
                : deviceName.trim();
        String sistema = platform == null || platform.isBlank()
                ? ""
                : " (" + platform.trim() + ")";
        enviar(
                recipient,
                "Confirma inicio de sesión - Guardian Escolar",
                "Confirma este dispositivo",
                "Se intentó iniciar sesión en " + dispositivo + sistema + ". Confirma el acceso si reconoces esta actividad.",
                "Confirmar acceso",
                url,
                "Si no reconoces este intento, ignora este email y cambia tu contraseña.");
    }

    public boolean enviarCodeDosFactores(String recipient, String code) {
        return enviarCodigo(
                recipient,
                "Código de verificación - Guardian Escolar",
                "Tu código de verificación",
                "Usa este código para completar la autenticación de dos factores. Vence en 10 minutos.",
                code,
                "Si no solicitaste este código, cambia tu contraseña.");
    }

    public boolean enviarNotification(String recipient, String subject, String message) {
        enviar(
                recipient,
                subject,
                subject,
                message,
                "Abrir Guardian Escolar",
                emailLinkService.frontendBaseUrl(),
                "Este email fue enviado por GPS Guardian Escolar.");
        return mailEnabled;
    }

    private void enviar(
            String recipient,
            String subject,
            String title,
            String description,
            String textoBoton,
            String url,
            String note) {
        if (!mailEnabled) {
            LOGGER.info("Email desactivado. provider={}, subject={}", mailProvider, subject);
            return;
        }

        emailSender().send(
                recipient,
                from,
                subject,
                emailTemplateService.plainAction(title, description, textoBoton, url, note),
                emailTemplateService.htmlAction(title, description, textoBoton, url, note));
    }

    private boolean enviarCodigo(
            String recipient,
            String subject,
            String title,
            String description,
            String code,
            String note) {
        if (!mailEnabled) {
            LOGGER.info("Email desactivado. provider={}, subject={}", mailProvider, subject);
            return false;
        }

        emailSender().send(
                recipient,
                from,
                subject,
                emailTemplateService.plainCode(title, description, code, note),
                emailTemplateService.htmlCode(title, description, code, note));
        return true;
    }

    private EmailSender emailSender() {
        return switch (mailProvider.trim().toLowerCase(Locale.ROOT)) {
            case "smtp" -> smtpEmailSender;
            case "brevo" -> brevoEmailSender;
            default -> throw new IllegalStateException(
                    "MAIL_PROVIDER no soportado: " + mailProvider + ". Usa smtp o brevo.");
        };
    }
}
