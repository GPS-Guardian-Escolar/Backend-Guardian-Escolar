package com.guardianescolar.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import org.junit.jupiter.api.Test;

class TwoFactorServiceDeliveryTest {
    @Test
    void emailChallengeDoesNotClaimCodeWasSentWhenDeliveryIsDisabled() {
        UserRepository users = mock(UserRepository.class);
        EmailService email = mock(EmailService.class);
        AuthValidationService validation = mock(AuthValidationService.class);
        TwoFactorService service = new TwoFactorService(users, email, validation);
        User user = new User();
        user.setEmail("ana@example.com");
        when(validation.normalizeTwoFactorMethod("EMAIL")).thenReturn("EMAIL");

        assertThrows(EmailUnavailableException.class, () -> service.request(user, "EMAIL"));

        verify(email).enviarCodeDosFactores(org.mockito.ArgumentMatchers.eq("ana@example.com"),
                org.mockito.ArgumentMatchers.any());
    }
}
