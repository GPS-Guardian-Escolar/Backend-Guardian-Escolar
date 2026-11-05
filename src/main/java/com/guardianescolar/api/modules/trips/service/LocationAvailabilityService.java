package com.guardianescolar.api.modules.trips.service;

import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.domain.TripStatus;
import com.guardianescolar.api.modules.trips.repository.CoordinateRepository;
import com.guardianescolar.api.modules.trips.repository.TripRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationAvailabilityService {
    private static final double MOVEMENT_START_THRESHOLD_METERS = 40.0;

    private final TripRepository tripRepository;
    private final CoordinateRepository coordinateRepository;
    private final StudentGuardianRepository guardianRepository;
    private final NotificationService notificationService;
    private final GeoMathService geoMathService;

    @Value("${guardian.gps.location-stale-seconds:120}")
    private long staleSeconds;

    @Scheduled(fixedDelayString = "${guardian.gps.location-check-ms:30000}")
    @Transactional
    public void checkActiveTrips() {
        OffsetDateTime now = OffsetDateTime.now();
        for (Trip trip : tripRepository.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)) {
            if (trip.getStatus() != TripStatus.IN_PROGRESS) continue;
            var lastCoordinate = coordinateRepository.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())
                    .stream().findFirst().orElse(null);
            var origin = coordinateRepository.findFirstByTripIdOrderByRecordedAtAsc(trip.getId());
            if (lastCoordinate == null || origin == null || geoMathService.distanceMeters(
                    origin.getLatitude(), origin.getLongitude(), lastCoordinate.getLatitude(), lastCoordinate.getLongitude())
                    < MOVEMENT_START_THRESHOLD_METERS) continue;
            OffsetDateTime lastLocation = lastCoordinate.getRecordedAt();
            if (lastLocation == null || lastLocation.isAfter(now.minusSeconds(staleSeconds))) continue;

            Map<UUID, User> recipients = new LinkedHashMap<>();
            User owner = trip.getStudent().getUser();
            if (owner != null && owner.getId() != null) recipients.put(owner.getId(), owner);
            guardianRepository.findByStudentIdAndStatusAndDeletedAtIsNull(trip.getStudent().getId(), "ACTIVE")
                    .forEach(guardian -> {
                        User user = guardian.getUser();
                        if (user != null && user.getId() != null) recipients.putIfAbsent(user.getId(), user);
                    });
            String message = "No recibimos ubicación reciente de " + trip.getStudent().getFullName()
                    + ". Puede haber pérdida de internet o GPS, o haberse detenido la ubicación compartida.";
            recipients.values().forEach(user -> notificationService.createForUserOnceSince(
                    trip, user, NotificationEventTypes.LOCATION_UNAVAILABLE, message, lastLocation));
        }
    }
}
