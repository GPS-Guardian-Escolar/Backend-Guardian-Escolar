package com.guardianescolar.api.modules.students.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.auth.service.EmailService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.StudentGuardian;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentGuardianServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private StudentGuardianService service;

    private Student student;
    private UUID guardianUserId;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        student = new Student();
        student.setId(UUID.randomUUID());
        student.setUser(owner);
        guardianUserId = UUID.randomUUID();
    }

    @Test
    void revokeMarksSharedAccessRevokedWithoutDeletingTheRow() {
        StudentGuardian access = new StudentGuardian();
        access.setStatus("ACTIVE");
        when(studentGuardianRepository.findByStudentIdAndUserIdAndStatusAndDeletedAtIsNull(
                student.getId(), guardianUserId, "ACTIVE"))
                .thenReturn(Optional.of(access));

        service.revoke(student, guardianUserId);

        assertEquals("REVOKED", access.getStatus());
        verify(studentGuardianRepository).save(access);
    }

    @Test
    void ownerCannotBeRemovedFromSharedAccess() {
        UUID ownerId = student.getUser().getId();

        assertThrows(IllegalArgumentException.class, () -> service.revoke(student, ownerId));

        verify(studentGuardianRepository, never())
                .findByStudentIdAndUserIdAndStatusAndDeletedAtIsNull(student.getId(), ownerId, "ACTIVE");
    }
}
