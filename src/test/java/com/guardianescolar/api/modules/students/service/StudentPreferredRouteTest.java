package com.guardianescolar.api.modules.students.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.domain.StudentRoute;
import com.guardianescolar.api.modules.routes.domain.StudentRouteId;
import com.guardianescolar.api.modules.routes.repository.RouteRepository;
import com.guardianescolar.api.modules.routes.repository.StudentRouteRepository;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.EmergencyContactRepository;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class StudentPreferredRouteTest {

    @Test
    void clearsCurrentPreferenceBeforeMarkingTheNextRoute() {
        UUID studentId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        StudentRepository students = mock(StudentRepository.class);
        StudentRouteRepository assignments = mock(StudentRouteRepository.class);
        Student student = mock(Student.class);
        Route route = mock(Route.class);
        StudentRoute assignment = mock(StudentRoute.class);

        when(students.findByIdAndDeletedAtIsNull(studentId)).thenReturn(Optional.of(student));
        when(assignments.findByIdStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of(assignment));
        when(assignment.getRoute()).thenReturn(route);
        when(route.getId()).thenReturn(routeId);
        when(assignment.getId()).thenReturn(new StudentRouteId(studentId, routeId));
        when(assignments.markPreferredRoute(studentId, routeId)).thenReturn(1);

        StudentService service = new StudentService(
                students,
                mock(EmergencyContactRepository.class),
                mock(StudentDeviceRepository.class),
                mock(UserRepository.class),
                mock(RouteRepository.class),
                assignments,
                mock(CurrentUserService.class),
                mock(StudentMapper.class),
                mock(StudentAccessService.class),
                mock(StudentGuardianService.class),
                mock(StudentDeviceLinkService.class),
                mock(StudentLinkCodeService.class));

        service.setPreferredRoute(studentId, routeId);

        InOrder order = inOrder(assignments);
        order.verify(assignments).clearPreferredRoute(studentId);
        order.verify(assignments).markPreferredRoute(studentId, routeId);
    }

    @Test
    void doesNotClearPreferenceWhenRouteIsNotAssignedToStudent() {
        UUID studentId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        StudentRepository students = mock(StudentRepository.class);
        StudentRouteRepository assignments = mock(StudentRouteRepository.class);
        when(students.findByIdAndDeletedAtIsNull(studentId)).thenReturn(Optional.of(mock(Student.class)));
        when(assignments.findByIdStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        StudentService service = new StudentService(
                students,
                mock(EmergencyContactRepository.class),
                mock(StudentDeviceRepository.class),
                mock(UserRepository.class),
                mock(RouteRepository.class),
                assignments,
                mock(CurrentUserService.class),
                mock(StudentMapper.class),
                mock(StudentAccessService.class),
                mock(StudentGuardianService.class),
                mock(StudentDeviceLinkService.class),
                mock(StudentLinkCodeService.class));

        assertThrows(IllegalArgumentException.class, () -> service.setPreferredRoute(studentId, routeId));
        verify(assignments).findByIdStudentIdAndIsActiveTrue(studentId);
        org.mockito.Mockito.verifyNoMoreInteractions(assignments);
    }
}
