package com.guardianescolar.api.modules.trips.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.domain.StudentRoute;
import com.guardianescolar.api.modules.routes.domain.StudentRouteId;
import com.guardianescolar.api.modules.routes.repository.StudentRouteRepository;
import com.guardianescolar.api.modules.routes.service.RouteService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import com.guardianescolar.api.modules.students.service.StudentAccessService;
import com.guardianescolar.api.modules.students.service.StudentService;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.domain.TripStatus;
import com.guardianescolar.api.modules.trips.dto.TripDtos;
import com.guardianescolar.api.modules.trips.repository.CoordinateRepository;
import com.guardianescolar.api.modules.trips.repository.TripRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TripPreferredRouteTest {
    private final StudentDeviceRepository devices = mock(StudentDeviceRepository.class);
    private final TripRepository trips = mock(TripRepository.class);
    private final StudentRouteRepository assignments = mock(StudentRouteRepository.class);
    private final StudentService students = mock(StudentService.class);
    private final RouteService routes = mock(RouteService.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final TripMapper mapper = new TripMapper();
    private final TripService service = new TripService(devices, trips, mock(CoordinateRepository.class), assignments,
            students, mock(StudentRepository.class), mock(StudentAccessService.class), routes, current,
            notifications, mock(TripAlertService.class), mapper, mock(GeoMathService.class), mock(SimpMessagingTemplate.class));

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private Student student() {
        User owner = new User(); owner.setId(UUID.randomUUID()); owner.setEmail("owner@example.com");
        Student student = new Student(); student.setId(UUID.randomUUID()); student.setFullName("Test Student"); student.setUser(owner);
        when(current.currentUser()).thenReturn(owner);
        when(students.getAllowed(student.getId())).thenReturn(student);
        when(trips.existsByStudentIdAndStatusAndDeletedAtIsNull(student.getId(), TripStatus.IN_PROGRESS)).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner", null, List.of()));
        return student;
    }
    private StudentRoute assignment(Student student, Route route, boolean preferred) {
        StudentRoute item = new StudentRoute(student, route); item.setIsPreferred(preferred); return item;
    }
    private Route route() { Route route = new Route(); route.setId(UUID.randomUUID()); route.setRouteName("Route"); return route; }

    @Test void startsNextTripOnGuardianPreferredRoute() {
        Student student = student(); Route route = route();
        StudentRoute preferred = assignment(student, route, true);
        when(assignments.findFirstByIdStudentIdAndIsActiveTrueAndIsPreferredTrue(student.getId())).thenReturn(Optional.of(preferred));
        when(routes.getAllowed(route.getId())).thenReturn(route);
        when(assignments.existsByIdStudentIdAndIdRouteIdAndIsActiveTrue(student.getId(), route.getId())).thenReturn(true);
        when(trips.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TripDtos.TripResponse response = service.create(new TripDtos.TripRequest(student.getId(), null, null));
        assertEquals(route.getId(), response.routeId());
        verify(routes).getAllowed(route.getId());
    }

    @Test void refusesToGuessWhenSeveralRoutesAreAssignedWithoutPreference() {
        Student student = student(); Route first = route(); Route second = route();
        when(assignments.findFirstByIdStudentIdAndIsActiveTrueAndIsPreferredTrue(student.getId())).thenReturn(Optional.empty());
        when(assignments.findByIdStudentIdAndIsActiveTrue(student.getId())).thenReturn(List.of(assignment(student, first, false), assignment(student, second, false)));
        assertThrows(IllegalArgumentException.class, () -> service.create(new TripDtos.TripRequest(student.getId(), null, null)));
        verify(trips, never()).save(any());
        verify(routes, never()).getAllowed(any());
    }

    @Test void keepsSingleRouteAutomaticForExistingStudent() {
        Student student = student(); Route only = route();
        when(assignments.findFirstByIdStudentIdAndIsActiveTrueAndIsPreferredTrue(student.getId())).thenReturn(Optional.empty());
        when(assignments.findByIdStudentIdAndIsActiveTrue(student.getId())).thenReturn(List.of(assignment(student, only, false)));
        when(routes.getAllowed(only.getId())).thenReturn(only);
        when(assignments.existsByIdStudentIdAndIdRouteIdAndIsActiveTrue(student.getId(), only.getId())).thenReturn(true);
        when(trips.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TripDtos.TripResponse response = service.create(new TripDtos.TripRequest(student.getId(), null, null));
        assertEquals(only.getId(), response.routeId());
    }
}
