package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceProviderTest {

    private SmtpEmailSender smtpEmailSender;
    private BrevoEmailSender brevoEmailSender;
    private EmailLinkService emailLinkService;
    private EmailTemplateService emailTemplateService;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        smtpEmailSender = mock(SmtpEmailSender.class);
        brevoEmailSender = mock(BrevoEmailSender.class);
        emailLinkService = mock(EmailLinkService.class);
        emailTemplateService = mock(EmailTemplateService.class);
        emailService = new EmailService(smtpEmailSender, brevoEmailSender, emailLinkService, emailTemplateService);
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "from", "from@example.com");
        when(emailLinkService.frontendBaseUrl()).thenReturn("https://frontend.example.com");
        when(emailTemplateService.plainAction(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("plain");
        when(emailTemplateService.htmlAction(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<p>html</p>");
    }

    @Test
    void selectsSmtpProvider() {
        ReflectionTestUtils.setField(emailService, "mailProvider", "smtp");

        emailService.enviarNotification("recipient@example.com", "Subject", "Message");

        verify(smtpEmailSender).send(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void selectsBrevoProvider() {
        ReflectionTestUtils.setField(emailService, "mailProvider", "brevo");

        emailService.enviarNotification("recipient@example.com", "Subject", "Message");

        verify(brevoEmailSender).send(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rejectsUnknownProviderClearly() {
        ReflectionTestUtils.setField(emailService, "mailProvider", "unknown");

        assertThrows(IllegalStateException.class,
                () -> emailService.enviarNotification("recipient@example.com", "Subject", "Message"));
    }
}
