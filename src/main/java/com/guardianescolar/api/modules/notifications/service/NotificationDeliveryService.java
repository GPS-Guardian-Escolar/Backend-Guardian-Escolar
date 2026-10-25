package com.guardianescolar.api.modules.notifications.service;

import com.guardianescolar.api.modules.notifications.domain.Notification;
import com.guardianescolar.api.modules.auth.service.EmailService;
import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.notifications.domain.NotificationSettings;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.trips.domain.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final ExpoPushService expoPushService;
    private final EmailService emailService;

    public boolean deliver(Notification notification, NotificationSettings settings, Trip trip, User user) {
        if (!eventEnabled(notification.getEventType(), settings)) {
            return false;
        }
        boolean sent = false;
        if (Boolean.TRUE.equals(settings.getPushChannel())) {
            sent = expoPushService.send(user, notification) || sent;
        }
        if (Boolean.TRUE.equals(settings.getEmailChannel())) {
            sent = emailService.enviarNotification(user.getEmail(), title(notification.getEventType()), notification.getMessage()) || sent;
        }
        return sent;
    }

    private boolean eventEnabled(String eventType, NotificationSettings settings) {
        return switch (eventType) {
            case NotificationEventTypes.DELAY -> Boolean.TRUE.equals(settings.getDelayAlert());
            case NotificationEventTypes.ROUTE_DEVIATION -> Boolean.TRUE.equals(settings.getRouteChangeAlert());
            case NotificationEventTypes.SAFE_ZONE_ENTRY, NotificationEventTypes.SAFE_ZONE_EXIT ->
                    Boolean.TRUE.equals(settings.getArrivalAlert());
            case NotificationEventTypes.INACTIVITY -> Boolean.TRUE.equals(settings.getInactivityAlert());
            default -> true;
        };
    }


    private String title(String eventType) {
        return switch (eventType) {
            case NotificationEventTypes.ROUTE_DEVIATION -> "Desvío de ruta";
            case NotificationEventTypes.SAFE_ZONE_ENTRY -> "Entrada a zona segura";
            case NotificationEventTypes.SAFE_ZONE_EXIT -> "Salida de zona segura";
            case NotificationEventTypes.TRIP_STARTED -> "Trayecto iniciado";
            case NotificationEventTypes.DELAY -> "Retraso del trayecto";
            case NotificationEventTypes.INACTIVITY -> "Inactividad detectada";
            default -> "Notificación de Guardian Escolar";
        };
    }

}

