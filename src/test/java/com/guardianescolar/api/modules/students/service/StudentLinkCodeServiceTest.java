package com.guardianescolar.api.modules.students.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class StudentLinkCodeServiceTest {

    @Test
    void issuedCodeCanBeVerifiedConsumedAndCannotBeReused() {
        PasswordEncoder encoder = new BCryptPasswordEncoder(4);
        StudentLinkCodeService realEncoderService = new StudentLinkCodeService(encoder, repository);
        StudentLinkCodeService.IssuedCode issued = realEncoderService.issue(student);
        assertEquals(16, issued.value().length());
        assertTrue(issued.expiresAt().isAfter(OffsetDateTime.now()));
        assertTrue(encoder.matches(issued.value(), student.getLinkCodeHash()));
        assertNotEquals(issued.value(), student.getLinkCodeHash());
        verify(repository).save(student);
        realEncoderService.verify(student, issued.value());
        realEncoderService.consume(student, issued.value());
        assertThrows(IllegalArgumentException.class, () -> realEncoderService.verify(student, issued.value()));
    }

    @Test
    void replacingCodeInvalidatesPreviousCode() {
        StudentLinkCodeService realEncoderService = new StudentLinkCodeService(new BCryptPasswordEncoder(4), repository);
        var previous = realEncoderService.issue(student);
        var replacement = realEncoderService.issue(student);
        assertThrows(IllegalArgumentException.class, () -> realEncoderService.verify(student, previous.value()));
        realEncoderService.verify(student, replacement.value());
    }

    private final StudentRepository repository = mock(StudentRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final StudentLinkCodeService service = new StudentLinkCodeService(passwordEncoder, repository);

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setFullName("Estudiante de prueba");
        student.setIsActive(true);
        student.setLinkCodeHash("encoded-code");
        student.setLinkCodeExpiresAt(OffsetDateTime.now().plusMinutes(10));
        student.setLinkCodeFailedAttempts(0);
        when(repository.findByDeletedAtIsNullOrderByFullNameAsc()).thenReturn(List.of(student));
    }

    @Test
    void resolvesActiveStudentWhenCodeDoesNotIncludeStudentId() {
        when(passwordEncoder.matches("ABCD1234", "encoded-code")).thenReturn(true);

        Student resolved = service.resolveStudent(" abcd1234 ");

        assertEquals(student, resolved);
    }

    @Test
    void rejectsInvalidCodeWithoutAStudentId() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.resolveStudent("INVALID1"));
    }

    @Test
    void rejectsExpiredCodeWithoutAStudentId() {
        student.setLinkCodeExpiresAt(OffsetDateTime.now().minusMinutes(1));

        assertThrows(IllegalArgumentException.class, () -> service.resolveStudent("ABCD1234"));
    }
}
