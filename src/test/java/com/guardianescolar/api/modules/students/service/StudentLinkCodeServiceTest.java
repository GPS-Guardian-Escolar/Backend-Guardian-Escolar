package com.guardianescolar.api.modules.students.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class StudentLinkCodeServiceTest {

    private PasswordEncoder encoder;
    private StudentRepository students;
    private StudentLinkCodeService service;

    @BeforeEach
    void setup() {
        encoder = mock(PasswordEncoder.class);
        students = mock(StudentRepository.class);
        service = new StudentLinkCodeService(encoder, students);
    }

    @Test
    void issuesRandomCodeAndStoresOnlyItsHash() {
        when(encoder.encode(any())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));

        Student student = new Student();
        StudentLinkCodeService.IssuedCode issued = service.issue(student);

        assertEquals(16, issued.value().length());
        assertTrue(issued.expiresAt().isAfter(OffsetDateTime.now()));
        assertEquals("hash:" + issued.value(), student.getLinkCodeHash());
        assertNull(student.getLinkCodeUsedAt());
        assertEquals(0, student.getLinkCodeFailedAttempts());
        verify(students).save(student);
    }

    @Test
    void consumesCodeOnlyOnce() {
        Student student = validStudent();
        when(encoder.matches("VALIDCODE", "stored-hash")).thenReturn(true);

        service.consume(student, "VALIDCODE");

        assertNotNull(student.getLinkCodeUsedAt());
        verify(students).save(student);
        assertThrows(IllegalArgumentException.class, () -> service.consume(student, "VALIDCODE"));
    }

    @Test
    void locksAfterFiveInvalidAttempts() {
        Student student = validStudent();
        when(encoder.matches(any(), eq("stored-hash"))).thenReturn(false);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(IllegalArgumentException.class, () -> service.verify(student, "WRONGCODE"));
        }

        assertEquals(5, student.getLinkCodeFailedAttempts());
        verify(encoder, times(5)).matches("WRONGCODE", "stored-hash");
        assertThrows(IllegalArgumentException.class, () -> service.verify(student, "WRONGCODE"));
        verify(encoder, times(5)).matches("WRONGCODE", "stored-hash");
    }

    private Student validStudent() {
        Student student = new Student();
        student.setLinkCodeHash("stored-hash");
        student.setLinkCodeExpiresAt(OffsetDateTime.now().plusMinutes(5));
        student.setLinkCodeFailedAttempts(0);
        return student;
    }
}