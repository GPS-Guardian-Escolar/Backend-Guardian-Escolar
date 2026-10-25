package com.guardianescolar.api.modules.notifications.service;

import com.guardianescolar.api.modules.notifications.domain.Notification;
import com.guardianescolar.api.modules.notifications.domain.NotificationReceipt;
import com.guardianescolar.api.modules.notifications.domain.NotificationSettings;
import com.guardianescolar.api.modules.notifications.dto.NotificationDtos;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDtos.NotificationSettingsResponse toSettingsResponse(
            NotificationSettings settings) {
        return new NotificationDtos.NotificationSettingsResponse(
                settings.getDelayAlert(),
                settings.getRouteChangeAlert(),
                settings.getArrivalAlert(),
                settings.getInactivityAlert(),
                settings.getPushChannel(),
                settings.getEmailChannel());
    }

    public NotificationDtos.NotificationResponse toResponse(NotificationReceipt receipt) {
        Notification notification = receipt.getNotification();
        return new NotificationDtos.NotificationResponse(
                receipt.getId(),
                notification.getId(),
                notification.getEventType(),
                notification.getMessage(),
                notification.getEventDateTime(),
                notification.getWasSent(),
                receipt.getWasRead(),
                receipt.getReceivedAt(),
                receipt.getReadAt(),
                notification.getTrip() == null ? null : notification.getTrip().getId());
    }
}
