package com.guardianescolar.api.modules.trips.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.StudentGuardian;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;
import com.guardianescolar.api.modules.trips.domain.Coordinate;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.domain.TripStatus;
import com.guardianescolar.api.modules.trips.repository.CoordinateRepository;
import com.guardianescolar.api.modules.trips.repository.TripRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LocationAvailabilityServiceTest {
    @Mock TripRepository trips;
    @Mock CoordinateRepository coordinates;
    @Mock StudentGuardianRepository guardians;
    @Mock NotificationService notifications;
    LocationAvailabilityService service;

    @BeforeEach void setUp() {
        service = new LocationAvailabilityService(trips, coordinates, guardians, notifications, new GeoMathService());
        ReflectionTestUtils.setField(service, "staleSeconds", 120L);
    }

    @Test void staleTripNotifiesOwnerAndEachActiveGuardianOnce() {
        User owner = user();
        User guardian = user();
        Student student = new Student(); student.setId(UUID.randomUUID()); student.setFullName("Sara"); student.setUser(owner);
        Trip trip = new Trip(); trip.setId(UUID.randomUUID()); trip.setStudent(student); trip.setTripStartedAt(OffsetDateTime.now().minusMinutes(10));
        trip.setStatus(TripStatus.IN_PROGRESS);
        StudentGuardian shared = new StudentGuardian(); shared.setUser(guardian);
        StudentGuardian duplicate = new StudentGuardian(); duplicate.setUser(owner);
        Coordinate origin = coordinate(0.0, 0.0, OffsetDateTime.now().minusMinutes(10));
        Coordinate last = coordinate(0.001, 0.0, OffsetDateTime.now().minusMinutes(3));
        when(trips.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(coordinates.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())).thenReturn(List.of(last));
        when(coordinates.findFirstByTripIdOrderByRecordedAtAsc(trip.getId())).thenReturn(origin);
        when(guardians.findByStudentIdAndStatusAndDeletedAtIsNull(student.getId(), "ACTIVE")).thenReturn(List.of(shared, duplicate));

        service.checkActiveTrips();

        verify(notifications).createForUserOnceSince(eq(trip), eq(owner), eq(NotificationEventTypes.LOCATION_UNAVAILABLE), contains("Puede haber"), eq(last.getRecordedAt()));
        verify(notifications).createForUserOnceSince(eq(trip), eq(guardian), eq(NotificationEventTypes.LOCATION_UNAVAILABLE), contains("Puede haber"), eq(last.getRecordedAt()));
        verifyNoMoreInteractions(notifications);
    }

    @Test void recentLocationDoesNotSendAnAlert() {
        Trip trip = new Trip(); trip.setId(UUID.randomUUID()); trip.setTripStartedAt(OffsetDateTime.now().minusMinutes(5));
        trip.setStatus(TripStatus.IN_PROGRESS);
        Coordinate origin = coordinate(0.0, 0.0, OffsetDateTime.now().minusMinutes(5));
        Coordinate last = coordinate(0.001, 0.0, OffsetDateTime.now().minusSeconds(30));
        when(trips.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(coordinates.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())).thenReturn(List.of(last));
        when(coordinates.findFirstByTripIdOrderByRecordedAtAsc(trip.getId())).thenReturn(origin);
        service.checkActiveTrips();
        verifyNoInteractions(notifications, guardians);
    }

    @Test void activeTripWithoutMovementDoesNotAlertWhenNoCoordinatesExist() {
        Trip trip = new Trip(); trip.setId(UUID.randomUUID()); trip.setTripStartedAt(OffsetDateTime.now().minusMinutes(3));
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(trips.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(coordinates.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())).thenReturn(List.of());
        service.checkActiveTrips();
        verifyNoInteractions(notifications, guardians);
    }

    @Test void pendingTripDoesNotNotifyWhenThePhoneHasNotStartedMoving() {
        Trip pending = new Trip(); pending.setId(UUID.randomUUID()); pending.setTripStartedAt(OffsetDateTime.now().minusHours(2));
        when(trips.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)).thenReturn(List.of(pending));

        service.checkActiveTrips();

        verifyNoInteractions(coordinates, guardians, notifications);
    }

    private User user() { User user = new User(); user.setId(UUID.randomUUID()); return user; }

    private Coordinate coordinate(double latitude, double longitude, OffsetDateTime recordedAt) {
        Coordinate coordinate = new Coordinate();
        coordinate.setLatitude(java.math.BigDecimal.valueOf(latitude));
        coordinate.setLongitude(java.math.BigDecimal.valueOf(longitude));
        coordinate.setRecordedAt(recordedAt);
        return coordinate;
    }
}
