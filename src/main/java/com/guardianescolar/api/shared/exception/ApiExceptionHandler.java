package com.guardianescolar.api.shared.exception;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.audit.service.ErrorLogService;
import com.guardianescolar.api.modules.maps.exception.MapsApiException;
import com.guardianescolar.api.modules.maps.exception.MapsRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final ErrorLogService errorLogService;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ApiError(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Solicitud inválida",
                errors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        return build(HttpStatus.CONFLICT, "La operación viola una restricción de datos");
    }

    @ExceptionHandler(MapsApiException.class)
    public ResponseEntity<ApiError> handleMapsApi(MapsApiException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(exception.status());
        if (exception.retryAfter() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfter().toSeconds()));
        }
        return builder.body(new ApiError(
                OffsetDateTime.now(),
                exception.status().value(),
                exception.getMessage(),
                List.of(exception.googleStatus())));
    }

    @ExceptionHandler(MapsRateLimitException.class)
    public ResponseEntity<ApiError> handleMapsRateLimit(MapsRateLimitException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfter().toSeconds()))
                .body(new ApiError(
                        OffsetDateTime.now(),
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        exception.getMessage(),
                        List.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para ejecutar esta acción");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        errorLogService.register(exception, request);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(
                OffsetDateTime.now(),
                status.value(),
                message,
                List.of()));
    }

    public record ApiError(
            OffsetDateTime timestamp,
            int status,
            String message,
            List<String> errors) {
    }
}
