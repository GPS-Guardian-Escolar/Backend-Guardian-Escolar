package com.guardianescolar.api.modules.notifications.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.guardianescolar.api.modules.notifications.domain.UserDevice;
import com.guardianescolar.api.modules.notifications.domain.Notification;
import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.notifications.repository.UserDeviceRepository;
import com.guardianescolar.api.modules.security.domain.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class ExpoPushService {

    private final UserDeviceRepository userDeviceRepository;
    private final RestClient restClient;
    private final boolean enabled;

    public ExpoPushService(
            UserDeviceRepository userDeviceRepository,
            RestClient.Builder restClientBuilder,
            @Value("${guardian.push.expo.enabled:false}") boolean enabled,
            @Value("${guardian.push.expo.url:https://exp.host/--/api/v2/push/send}") String expoPushUrl) {
        this.userDeviceRepository = userDeviceRepository;
        this.restClient = restClientBuilder.baseUrl(expoPushUrl).build();
        this.enabled = enabled;
    }

    @Transactional
    public boolean send(User user, Notification notification) {
        if (!enabled || user == null) {
            return false;
        }

        List<UserDevice> devices = userDeviceRepository
                .findByUserIdAndIsActiveTrueAndDeletedAtIsNull(user.getId());
        boolean successfulDelivery = false;
        for (UserDevice device : devices) {
            successfulDelivery = sendToDevice(device, notification) || successfulDelivery;
        }
        return successfulDelivery;
    }

    private boolean sendToDevice(UserDevice device, Notification notification) {
        Map<String, Object> body = Map.of(
                "to", device.getExpoPushToken(),
                "sound", "default",
                "title", title(notification.getEventType()),
                "body", notification.getMessage(),
                "data", Map.of(
                        "NotificationId", notification.getId().toString(),
                        "eventType", notification.getEventType()));

        try {
            JsonNode response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String status = response == null || response.get("data") == null
                    ? null
                    : response.get("data").path("status").asText(null);
            boolean ok = "ok".equalsIgnoreCase(status);
            if (ok) {
                device.setLastUsedAt(OffsetDateTime.now());
                userDeviceRepository.save(device);
            } else {
                log.warn("Expo Push did not confirm delivery for device {}: {}", device.getId(), response);
            }
            return ok;
        } catch (RuntimeException exception) {
            log.warn("Could not send Expo Push to device {}", device.getId(), exception);
            return false;
        }
    }

    private String title(String eventType) {
        return switch (eventType) {
            case NotificationEventTypes.INACTIVITY -> "Inactivity alert";
            case NotificationEventTypes.LOCATION_UNAVAILABLE -> "Ubicación no disponible";
            case NotificationEventTypes.LOCATION_STOPPED -> "Seguimiento detenido";
            case NotificationEventTypes.ROUTE_DEVIATION -> "Route deviation alert";
            case NotificationEventTypes.SAFE_ZONE_ENTRY -> "Safe zone arrival";
            case NotificationEventTypes.SAFE_ZONE_EXIT -> "Safe zone departure";
            case NotificationEventTypes.TRIP_STARTED -> "Trayecto iniciado";
            case NotificationEventTypes.TRIP_FINISHED -> "Trayecto finalizado";
            case NotificationEventTypes.DEVICE_LINKED -> "Celular vinculado";
            default -> "Guardian Escolar";
        };
    }
}
