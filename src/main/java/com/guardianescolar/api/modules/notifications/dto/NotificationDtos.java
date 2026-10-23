package com.guardianescolar.api.modules.notifications.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            UUID receiptId,
            UUID notificationId,
            String eventType,
            String message,
            OffsetDateTime eventDateTime,
            Boolean wasSent,
            Boolean wasRead,
            OffsetDateTime receivedAt,
            OffsetDateTime readAt,
            UUID tripId) {
    }

    public record NotificationSettingsResponse(
            Boolean delayAlert,
            Boolean routeChangeAlert,
            Boolean arrivalAlert,
            Boolean inactivityAlert,
            Boolean pushChannel,
            Boolean emailChannel,
            Boolean smsChannel,
            Boolean smsAvailable) {
    }

    public record UpdateNotificationSettingsRequest(
            Boolean delayAlert,
            Boolean routeChangeAlert,
            Boolean arrivalAlert,
            Boolean inactivityAlert,
            Boolean pushChannel,
            Boolean emailChannel,
            Boolean smsChannel) {
    }

    public record DeviceRequest(
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(ExponentPushToken|ExpoPushToken)\\[[A-Za-z0-9_-]+]$", message = "expoPushToken debe tener formato ExpoPushToken[...]")
            String expoPushToken,
            @NotBlank
            @Pattern(regexp = "ANDROID|IOS|WEB", message = "platform debe ser ANDROID, IOS o WEB")
            String platform,
            @Size(max = 120)
            String deviceName) {
    }

    public record DeviceResponse(
            UUID id,
            String expoPushToken,
            String platform,
            String deviceName,
            Boolean isActive,
            OffsetDateTime lastUsedAt) {
    }
}
