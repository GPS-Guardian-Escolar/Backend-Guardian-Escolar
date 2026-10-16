package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailSenderTest {

    @Test
    void sendsHtmlAndPlainTextThroughJavaMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        SmtpEmailSender sender = new SmtpEmailSender(mailSender);

        assertDoesNotThrow(() -> sender.send(
                "recipient@example.com",
                "from@example.com",
                "Subject",
                "Plain text",
                "<p>HTML</p>"));

        verify(mailSender).send(message);
    }
}
