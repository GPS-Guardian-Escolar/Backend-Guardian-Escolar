package com.guardianescolar.api.modules.students.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class StudentAccessServiceTest {
    @Test
    void sharedViewerCanReadButCannotManageStudentOrIssueCodes() {
        CurrentUserService current = mock(CurrentUserService.class);
        StudentGuardianRepository guardians = mock(StudentGuardianRepository.class);
        StudentAccessService service = new StudentAccessService(current, guardians);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        User viewer = new User();
        viewer.setId(UUID.randomUUID());
        Student student = new Student();
        student.setId(UUID.randomUUID());
        student.setUser(owner);
        when(current.currentUser()).thenReturn(viewer);
        when(guardians.existsByStudentIdAndUserIdAndStatusAndDeletedAtIsNull(student.getId(), viewer.getId(), "ACTIVE"))
                .thenReturn(true);
        assertDoesNotThrow(() -> service.validateAccess(student));
        assertThrows(AccessDeniedException.class, () -> service.validateManagement(student));
        when(current.currentUser()).thenReturn(owner);
        assertDoesNotThrow(() -> service.validateManagement(student));
        when(current.currentUser()).thenReturn(viewer);
        when(current.isAdmin(viewer)).thenReturn(true);
        assertDoesNotThrow(() -> service.validateManagement(student));
    }
}
