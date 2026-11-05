package com.guardianescolar.api.modules.trips.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.modules.routes.repository.StopRepository;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;
import com.guardianescolar.api.modules.trips.domain.Coordinate;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.repository.CoordinateRepository;
import com.guardianescolar.api.modules.trips.repository.TripRepository;
import com.guardianescolar.api.modules.zones.repository.SafeZoneRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripAlertServiceTest {

    @Test
    void ignoresRouteAndZoneAlertsForAStationaryHeartbeat() {
        CoordinateRepository coordinates = mock(CoordinateRepository.class);
        TripRepository trips = mock(TripRepository.class);
        SafeZoneRepository zones = mock(SafeZoneRepository.class);
        StopRepository stops = mock(StopRepository.class);
        StudentGuardianRepository guardians = mock(StudentGuardianRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        GeoMathService geoMath = mock(GeoMathService.class);
        Student student = new Student();
        student.setId(UUID.randomUUID());
        Trip trip = new Trip();
        trip.setStudent(student);
        trip.setStatus(com.guardianescolar.api.modules.trips.domain.TripStatus.IN_PROGRESS);
        Coordinate heartbeat = new Coordinate();
        heartbeat.setStoppedSeconds(30);
        when(zones.findByStudentIdAndIsActiveTrueAndDeletedAtIsNull(student.getId())).thenReturn(List.of());

        TripAlertService service = new TripAlertService(
                coordinates, trips, zones, stops, guardians, notifications, geoMath);

        service.processCoordinate(trip, heartbeat);

        verify(coordinates, never()).findTop3ByTripIdOrderByRecordedAtDesc(trip.getId());
        verify(trips, never()).save(trip);
        verifyNoInteractions(notifications);
    }

    @Test
    void stationaryFixConfirmsArrivalAfterEnteringTheZone() {
        CoordinateRepository coordinates = mock(CoordinateRepository.class);
        TripRepository trips = mock(TripRepository.class);
        SafeZoneRepository zones = mock(SafeZoneRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        GeoMathService geo = mock(GeoMathService.class);
        Student student = new Student();
        student.setId(UUID.randomUUID());
        var owner = new com.guardianescolar.api.modules.security.domain.User();
        owner.setId(UUID.randomUUID());
        student.setUser(owner);
        Trip trip = new Trip(); trip.setId(UUID.randomUUID()); trip.setStudent(student);
        trip.setStatus(com.guardianescolar.api.modules.trips.domain.TripStatus.IN_PROGRESS);
        var zone = new com.guardianescolar.api.modules.zones.domain.SafeZone();
        zone.setId(UUID.randomUUID()); zone.setZoneName("School"); zone.setInactivityAlertSeconds(300);
        zone.setLatitude(java.math.BigDecimal.ZERO); zone.setLongitude(java.math.BigDecimal.ZERO); zone.setRadiusMeters(100);
        Coordinate outside = new Coordinate(); outside.setId(UUID.randomUUID());
        outside.setLatitude(java.math.BigDecimal.ONE); outside.setLongitude(java.math.BigDecimal.ZERO);
        Coordinate entry = new Coordinate(); entry.setId(UUID.randomUUID());
        entry.setLatitude(java.math.BigDecimal.ZERO); entry.setLongitude(java.math.BigDecimal.ZERO);
        Coordinate stopped = new Coordinate(); stopped.setId(UUID.randomUUID());
        stopped.setLatitude(java.math.BigDecimal.ZERO); stopped.setLongitude(java.math.BigDecimal.ZERO); stopped.setStoppedSeconds(30);
        when(coordinates.findFirstByTripIdOrderByRecordedAtAsc(trip.getId())).thenReturn(outside);
        when(zones.findByStudentIdAndIsActiveTrueAndDeletedAtIsNull(student.getId())).thenReturn(List.of(zone));
        when(coordinates.findTop3ByTripIdOrderByRecordedAtDesc(trip.getId())).thenReturn(List.of(stopped, entry, outside));
        when(geo.isInsideRadius(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 100)).thenReturn(true);
        when(geo.distanceMeters(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).thenReturn(111_000.0);
        var service = new TripAlertService(coordinates, trips, zones, mock(StopRepository.class),
                mock(StudentGuardianRepository.class), notifications, geo);
        service.processCoordinate(trip, stopped);
        verify(notifications).createForUser(org.mockito.ArgumentMatchers.eq(trip), org.mockito.ArgumentMatchers.eq(zone),
                org.mockito.ArgumentMatchers.eq(owner), org.mockito.ArgumentMatchers.eq("SAFE_ZONE_ENTRY"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void pendingTripCannotGenerateMovementAlertsBeforeMovementStarts() {
        CoordinateRepository coordinates = mock(CoordinateRepository.class);
        TripRepository trips = mock(TripRepository.class);
        SafeZoneRepository zones = mock(SafeZoneRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        Student student = new Student();
        student.setId(UUID.randomUUID());
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setStudent(student);
        trip.setStatus(com.guardianescolar.api.modules.trips.domain.TripStatus.PENDING);
        Coordinate point = new Coordinate();
        point.setStoppedSeconds(900);

        TripAlertService service = new TripAlertService(
                coordinates, trips, zones, mock(StopRepository.class),
                mock(StudentGuardianRepository.class), notifications, mock(GeoMathService.class));

        service.processCoordinate(trip, point);
        service.notifyTrackingStopped(trip);

        verifyNoInteractions(notifications, zones, coordinates);
    }
}
