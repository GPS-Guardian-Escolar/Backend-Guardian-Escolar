package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.Mockito;

import com.guardianescolar.api.modules.auth.controller.AuthController;
import com.guardianescolar.api.modules.notifications.repository.NotificationSettingsRepository;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.shared.security.JwtService;
import com.guardianescolar.api.shared.security.TransientTokenHashService;

class EmailProviderSpringContextTest {

    private ConfigurableApplicationContext context;
    private HttpServer fakeBrevoServer;

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
        if (fakeBrevoServer != null) {
            fakeBrevoServer.stop(0);
        }
    }

    @ParameterizedTest(name = "provider={0}, enabled={1}")
    @MethodSource("providerConfigurations")
    void startsSpringContextWithEachMailConfiguration(String provider, boolean enabled) throws Exception {
        int fakeBrevoPort = startFakeBrevoServer();
        context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.profiles.active=prod",
                        "MAIL_PROVIDER=" + provider,
                        "APP_MAIL_ENABLED=" + enabled,
                        "BREVO_API_KEY=fake-brevo-key",
                        "BREVO_API_URL=http://localhost:" + fakeBrevoPort + "/v3/smtp/email",
                        "MAIL_HOST=localhost",
                        "MAIL_PORT=1025",
                        "MAIL_SMTP_AUTH=false",
                        "MAIL_SMTP_STARTTLS=false")
                .run();

        assertNotNull(context.getBean(BrevoEmailSender.class));
        assertNotNull(context.getBean(SmtpEmailSender.class));
        assertNotNull(context.getBean(EmailService.class));
        assertNotNull(context.getBean(AuthService.class));
        assertNotNull(context.getBean(AuthController.class));
    }

    private static Stream<Arguments> providerConfigurations() {
        return Stream.of(
                Arguments.of("brevo", true),
                Arguments.of("smtp", true),
                Arguments.of("smtp", false));
    }

    private int startFakeBrevoServer() throws Exception {
        fakeBrevoServer = HttpServer.create(new InetSocketAddress(0), 0);
        fakeBrevoServer.createContext("/v3/smtp/email", exchange -> {
            byte[] response = "{\"messageId\":\"context-test\"}".getBytes();
            exchange.sendResponseHeaders(201, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        fakeBrevoServer.start();
        return fakeBrevoServer.getAddress().getPort();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class
    })
    @Import({
            AuthController.class,
            AuthService.class,
            TransientTokenHashService.class,
            BrevoEmailSender.class,
            SmtpEmailSender.class,
            EmailService.class,
            EmailLinkService.class,
            EmailTemplateService.class
    })
    static class TestApplication {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        NotificationSettingsRepository notificationSettingsRepository() {
            return Mockito.mock(NotificationSettingsRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        JwtService jwtService() {
            return Mockito.mock(JwtService.class);
        }

        @Bean
        AuthenticationManager authenticationManager() {
            return Mockito.mock(AuthenticationManager.class);
        }

        @Bean
        CurrentUserService currentUserService() {
            return Mockito.mock(CurrentUserService.class);
        }

        @Bean
        AuthValidationService authValidationService() {
            return Mockito.mock(AuthValidationService.class);
        }

        @Bean
        AuthUserMapper authUserMapper() {
            return Mockito.mock(AuthUserMapper.class);
        }

        @Bean
        RoleProvisioningService roleProvisioningService() {
            return Mockito.mock(RoleProvisioningService.class);
        }

        @Bean
        DeviceSessionService deviceSessionService() {
            return Mockito.mock(DeviceSessionService.class);
        }

        @Bean
        TwoFactorService twoFactorService() {
            return Mockito.mock(TwoFactorService.class);
        }

        @Bean
        AuthAttemptLimiter authAttemptLimiter() {
            return Mockito.mock(AuthAttemptLimiter.class);
        }
    }
}
