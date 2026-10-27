package com.guardianescolar.api.modules.trips.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.modules.routes.repository.StudentRouteRepository;
import com.guardianescolar.api.modules.routes.service.RouteService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.StudentDevice;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import com.guardianescolar.api.modules.students.service.StudentAccessService;
import com.guardianescolar.api.modules.students.service.StudentService;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.domain.TripStatus;
import com.guardianescolar.api.modules.trips.dto.TripDtos;
import com.guardianescolar.api.modules.trips.repository.CoordinateRepository;
import com.guardianescolar.api.modules.trips.repository.TripRepository;
import com.guardianescolar.api.shared.security.ChildDeviceAuthenticationDetails;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ChildTripAccessTest {
    private final StudentDeviceRepository devices = mock(StudentDeviceRepository.class);
    private final TripRepository trips = mock(TripRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final TripService service = new TripService(devices, trips, mock(CoordinateRepository.class),
            mock(StudentRouteRepository.class), mock(StudentService.class), mock(StudentRepository.class),
            mock(StudentAccessService.class), mock(RouteService.class), current,
            mock(NotificationService.class), mock(TripAlertService.class), mock(TripMapper.class),
            mock(GeoMathService.class), mock(SimpMessagingTemplate.class));
    private Student student;
    private StudentDevice device;
    private Trip trip;

    @BeforeEach
    void setup() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        student = new Student();
        student.setId(UUID.randomUUID());
        student.setUser(owner);
        device = new StudentDevice();
        device.setStudent(student);
        device.setIsActive(true);
        trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setStudent(student);
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(trips.findByIdAndDeletedAtIsNull(trip.getId())).thenReturn(Optional.of(trip));
        when(devices.findByDeviceIdentifier("phone-1")).thenReturn(Optional.of(device));
        when(current.currentUser()).thenReturn(owner);
        var authentication = new UsernamePasswordAuthenticationToken("owner", null, List.of());
        authentication.setDetails(new ChildDeviceAuthenticationDetails(student.getId(), "phone-1"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void linkedDeviceCanFinishItsTrip() {
        service.changeStatus(trip.getId(), new TripDtos.ChangeStatusRequest(TripStatus.COMPLETED, null));
        assertEquals(TripStatus.COMPLETED, trip.getStatus());
        assertNotNull(trip.getTripEndedAt());
        verify(trips).save(trip);
    }

    @Test
    void revokedDeviceCannotFinishOrUpload() {
        device.setIsActive(false);
        assertThrows(AccessDeniedException.class, () -> service.changeStatus(trip.getId(),
                new TripDtos.ChangeStatusRequest(TripStatus.COMPLETED, null)));
        assertThrows(AccessDeniedException.class, () -> service.registerCoordinate(trip.getId(), null));
        verify(trips, never()).save(any());
    }

    @Test
    void deviceCannotUseAnotherStudentsTrip() {
        Student other = new Student();
        other.setId(UUID.randomUUID());
        trip.setStudent(other);
        assertThrows(AccessDeniedException.class, () -> service.changeStatus(trip.getId(),
                new TripDtos.ChangeStatusRequest(TripStatus.COMPLETED, null)));
        verify(trips, never()).save(any());
    }

    @Test
    void deviceCannotEraseIncidentsOrReopenTrip() {
        assertThrows(AccessDeniedException.class, () -> service.changeStatus(trip.getId(),
                new TripDtos.ChangeStatusRequest(TripStatus.COMPLETED, false)));
        assertThrows(AccessDeniedException.class, () -> service.changeStatus(trip.getId(),
                new TripDtos.ChangeStatusRequest(TripStatus.IN_PROGRESS, null)));
        verify(trips, never()).save(any());
    }
}
