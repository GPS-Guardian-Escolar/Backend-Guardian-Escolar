package com.guardianescolar.api.modules.notifications.controller;

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
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDtos.NotificationResponse> list() {
        return notificationService.listForCurrentUser();
    }

    @PatchMapping("/{receiptId}/read")
    public NotificationDtos.NotificationResponse markAsRead(@PathVariable UUID receiptId) {
        return notificationService.markAsRead(receiptId);
    }

    @PatchMapping("/read-all")
    public int markAllAsRead() {
        return notificationService.markAllAsRead();
    }

    @GetMapping("/settings")
    public NotificationDtos.NotificationSettingsResponse getSettings() {
        return notificationService.getCurrentSettings();
    }

    @PutMapping("/settings")
    public NotificationDtos.NotificationSettingsResponse updateSettings(
            @RequestBody NotificationDtos.UpdateNotificationSettingsRequest request) {
        return notificationService.updateCurrentSettings(request);
    }
}
