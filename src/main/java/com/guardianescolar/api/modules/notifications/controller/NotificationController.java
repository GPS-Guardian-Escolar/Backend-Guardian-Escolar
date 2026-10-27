package com.guardianescolar.api.modules.notifications.controller;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.modules.notifications.dto.NotificationDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications and read state")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o parámetros no válidos"),
    @ApiResponse(responseCode = "401", description = "Se requiere autenticación"),
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
})
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Listar recursos")
    @GetMapping
    public List<NotificationDtos.NotificationResponse> list() {
        return notificationService.listForCurrentUser();
    }

    @Operation(summary = "Marcar notificación como leída")
    @PatchMapping("/{receiptId}/read")
    public NotificationDtos.NotificationResponse markAsRead(@PathVariable UUID receiptId) {
        return notificationService.markAsRead(receiptId);
    }

    @Operation(summary = "Marcar todas las notificaciones como leídas")
    @PatchMapping("/read-all")
    public int markAllAsRead() {
        return notificationService.markAllAsRead();
    }

    @Operation(summary = "Consultar preferencias de notificación")
    @GetMapping("/settings")
    public NotificationDtos.NotificationSettingsResponse getSettings() {
        return notificationService.getCurrentSettings();
    }

    @Operation(summary = "Actualizar preferencias de notificación")
    @PutMapping("/settings")
    public NotificationDtos.NotificationSettingsResponse updateSettings(
            @RequestBody NotificationDtos.UpdateNotificationSettingsRequest request) {
        return notificationService.updateCurrentSettings(request);
    }
}
