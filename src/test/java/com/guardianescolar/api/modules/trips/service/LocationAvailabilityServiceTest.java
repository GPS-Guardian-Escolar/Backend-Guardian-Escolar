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
        service = new LocationAvailabilityService(trips, coordinates, guardians, notifications);
        ReflectionTestUtils.setField(service, "staleSeconds", 120L);
    }

    @Test void staleTripNotifiesOwnerAndEachActiveGuardianOnce() {
        User owner = user();
        User guardian = user();
        Student student = new Student(); student.setId(UUID.randomUUID()); student.setFullName("Sara"); student.setUser(owner);
        Trip trip = new Trip(); trip.setId(UUID.randomUUID()); trip.setStudent(student); trip.setTripStartedAt(OffsetDateTime.now().minusMinutes(10));
        StudentGuardian shared = new StudentGuardian(); shared.setUser(guardian);
        StudentGuardian duplicate = new StudentGuardian(); duplicate.setUser(owner);
        Coordinate last = new Coordinate(); last.setRecordedAt(OffsetDateTime.now().minusMinutes(3));
        when(trips.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(coordinates.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())).thenReturn(List.of(last));
        when(guardians.findByStudentIdAndStatusAndDeletedAtIsNull(student.getId(), "ACTIVE")).thenReturn(List.of(shared, duplicate));

        service.checkActiveTrips();

        verify(notifications).createForUserOnceSince(eq(trip), eq(owner), eq(NotificationEventTypes.LOCATION_UNAVAILABLE), contains("Puede haber"), eq(last.getRecordedAt()));
        verify(notifications).createForUserOnceSince(eq(trip), eq(guardian), eq(NotificationEventTypes.LOCATION_UNAVAILABLE), contains("Puede haber"), eq(last.getRecordedAt()));
        verifyNoMoreInteractions(notifications);
    }

    @Test void recentLocationDoesNotSendAnAlert() {
        Trip trip = new Trip(); trip.setId(UUID.randomUUID()); trip.setTripStartedAt(OffsetDateTime.now().minusMinutes(5));
        Coordinate last = new Coordinate(); last.setRecordedAt(OffsetDateTime.now().minusSeconds(30));
        when(trips.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(coordinates.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())).thenReturn(List.of(last));
        service.checkActiveTrips();
        verifyNoInteractions(notifications, guardians);
    }

    @Test void tripWithoutAnyLocationUsesTripStartForTimeout() {
        User owner = user();
        Student student = new Student(); student.setId(UUID.randomUUID()); student.setFullName("Sara"); student.setUser(owner);
        Trip trip = new Trip(); trip.setId(UUID.randomUUID()); trip.setStudent(student); trip.setTripStartedAt(OffsetDateTime.now().minusMinutes(3));
        when(trips.findByStatusAndDeletedAtIsNull(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(coordinates.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())).thenReturn(List.of());
        when(guardians.findByStudentIdAndStatusAndDeletedAtIsNull(student.getId(), "ACTIVE")).thenReturn(List.of());
        service.checkActiveTrips();
        verify(notifications).createForUserOnceSince(eq(trip), eq(owner), eq(NotificationEventTypes.LOCATION_UNAVAILABLE), anyString(), eq(trip.getTripStartedAt()));
    }

    private User user() { User user = new User(); user.setId(UUID.randomUUID()); return user; }
}
