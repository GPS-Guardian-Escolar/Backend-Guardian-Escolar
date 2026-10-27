package com.guardianescolar.api.modules.notifications.service;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.notifications.domain.Notification;
import com.guardianescolar.api.modules.notifications.domain.NotificationReceipt;
import com.guardianescolar.api.modules.notifications.domain.NotificationSettings;
import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.notifications.dto.NotificationDtos;
import com.guardianescolar.api.modules.notifications.repository.NotificationReceiptRepository;
import com.guardianescolar.api.modules.notifications.repository.NotificationRepository;
import com.guardianescolar.api.modules.notifications.repository.NotificationSettingsRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.zones.domain.SafeZone;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int INACTIVITY_DEDUPLICATION_MINUTES = 5;

    private final NotificationRepository notificationRepository;
    private final NotificationReceiptRepository notificationReceiptRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final CurrentUserService currentUserService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final NotificationMapper notificationMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> listForCurrentUser() {
        User current = currentUserService.currentUser();
        return notificationReceiptRepository.findByUserEmailIgnoreCaseOrderByReceivedAtDesc(current.getEmail())
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional
    public NotificationDtos.NotificationResponse markAsRead(UUID receiptId) {
        User current = currentUserService.currentUser();
        NotificationReceipt receipt = notificationReceiptRepository.findByIdAndUserEmailIgnoreCase(receiptId, current.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        receipt.markAsRead();
        return notificationMapper.toResponse(notificationReceiptRepository.save(receipt));
    }

    @Transactional
    public int markAllAsRead() {
        User current = currentUserService.currentUser();
        List<NotificationReceipt> receipts =
                notificationReceiptRepository.findByUserEmailIgnoreCaseAndWasReadFalse(current.getEmail());
        receipts.forEach(NotificationReceipt::markAsRead);
        notificationReceiptRepository.saveAll(receipts);
        return receipts.size();
    }

    @Transactional
    public NotificationDtos.NotificationSettingsResponse getCurrentSettings() {
        User current = currentUserService.currentUser();
        return settingsResponse(getOrInitializeSettings(current));
    }

    @Transactional
    public NotificationDtos.NotificationSettingsResponse updateCurrentSettings(
            NotificationDtos.UpdateNotificationSettingsRequest request) {
        User current = currentUserService.currentUser();
        NotificationSettings settings = getOrInitializeSettings(current);
        settings.setDelayAlert(newOrCurrent(request.delayAlert(), settings.getDelayAlert()));
        settings.setRouteChangeAlert(newOrCurrent(request.routeChangeAlert(), settings.getRouteChangeAlert()));
        settings.setArrivalAlert(newOrCurrent(request.arrivalAlert(), settings.getArrivalAlert()));
        settings.setInactivityAlert(newOrCurrent(request.inactivityAlert(), settings.getInactivityAlert()));
        settings.setPushChannel(newOrCurrent(request.pushChannel(), settings.getPushChannel()));
        settings.setEmailChannel(newOrCurrent(request.emailChannel(), settings.getEmailChannel()));
        return settingsResponse(notificationSettingsRepository.save(settings));
    }

    @Transactional
    public void createForUser(Trip trip, User user, String eventType, String message) {
        createForUser(trip, null, user, eventType, message);
    }

    @Transactional
    public void createForUser(
            Trip trip,
            SafeZone safeZone,
            User user,
            String eventType,
            String message) {
        if (NotificationEventTypes.INACTIVITY.equals(eventType)) {
            lockInactivityDeduplication(trip.getId(), eventType, user.getId());
            if (notificationRepository.existsRecentNotificationForUser(
                    trip.getId(), eventType,
                    OffsetDateTime.now().minusMinutes(INACTIVITY_DEDUPLICATION_MINUTES), user.getId())) {
                return;
            }
        }
        Notification notification = saveNotification(trip, safeZone, eventType, message);
        saveReceipt(notification, user);

        NotificationSettings settings = getOrInitializeSettings(user);
        if (notificationDeliveryService.deliver(notification, settings, trip, user)) {
            notification.setWasSent(true);
            notificationRepository.save(notification);
        }
    }

    private void lockInactivityDeduplication(UUID tripId, String eventType, UUID userId) {
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(:deduplicationKey, 0))")
                .setParameter("deduplicationKey", "notification:" + tripId + ":" + eventType + ":" + userId)
                .getSingleResult();
    }

    private Notification saveNotification(Trip trip, SafeZone safeZone, String eventType, String message) {
        Notification notification = new Notification();
        notification.setTrip(trip);
        notification.setSafeZone(safeZone);
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setWasSent(false);
        return notificationRepository.save(notification);
    }

    private void saveReceipt(Notification notification, User user) {
        NotificationReceipt receipt = new NotificationReceipt();
        receipt.setNotification(notification);
        receipt.setUser(user);
        notificationReceiptRepository.save(receipt);
    }

    private NotificationSettings getOrInitializeSettings(User user) {
        return notificationSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationSettings settings = new NotificationSettings();
                    settings.setUser(user);
                    return notificationSettingsRepository.save(settings);
                });
    }

    private NotificationDtos.NotificationSettingsResponse settingsResponse(NotificationSettings settings) {
        return notificationMapper.toSettingsResponse(settings);
    }

    private Boolean newOrCurrent(Boolean newValue, Boolean currentValue) {
        return newValue == null ? currentValue : newValue;
    }
}
