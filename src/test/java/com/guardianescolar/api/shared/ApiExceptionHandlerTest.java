package com.guardianescolar.api.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.guardianescolar.api.modules.audit.service.ErrorLogService;
import com.guardianescolar.api.modules.auth.service.EmailDeliveryException;
import com.guardianescolar.api.shared.exception.ApiExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {

    @Test
    void hidesProviderDetailsFromPublicEmailErrorResponse() {
        ErrorLogService errorLogService = mock(ErrorLogService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ApiExceptionHandler handler = new ApiExceptionHandler(errorLogService);
        EmailDeliveryException exception = new EmailDeliveryException(
                "brevo", 401, "Brevo rechazo el email con HTTP 401", null);

        var response = handler.handleEmailDelivery(exception, request);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("No se pudo enviar el correo. Intenta nuevamente más tarde.", response.getBody().message());
        assertFalse(response.getBody().message().contains("Brevo"));
        verify(errorLogService).register(exception, request);
    }

    @Test
    void mapsValidationArgumentErrorsToBadRequest() {
        ErrorLogService errorLogService = mock(ErrorLogService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ApiExceptionHandler handler = new ApiExceptionHandler(errorLogService);

        var response = handler.handleBadRequest(new IllegalArgumentException("validation"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("validation", response.getBody().message());
        verify(errorLogService).register(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request));
    }
}
