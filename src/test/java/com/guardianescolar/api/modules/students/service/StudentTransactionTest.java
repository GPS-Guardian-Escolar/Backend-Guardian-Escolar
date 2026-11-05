package com.guardianescolar.api.modules.students.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.routes.repository.RouteRepository;
import com.guardianescolar.api.modules.routes.repository.StudentRouteRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.modules.students.domain.EmergencyContact;
import com.guardianescolar.api.modules.students.domain.SchoolGrade;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.dto.EmergencyContactDtos;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import com.guardianescolar.api.modules.students.repository.EmergencyContactRepository;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/** Exercises real Spring transactions and database writes through repository test doubles.
 *  This checks the transaction boundary, not PostgreSQL/Hibernate schema compatibility. */
class StudentTransactionTest {
    private JdbcTemplate jdbc;
    private StudentRepository students;
    private EmergencyContactRepository contacts;
    private StudentService service;
    private User owner;
    private UserRepository users;
    private Student savedStudent;
    private EmergencyContact savedContact;
    private boolean failContact;

    @BeforeEach
    void setup() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:student-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table students (id uuid primary key, name varchar(60) not null)");
        jdbc.execute("create table contacts (id uuid primary key, student_id uuid references students(id), name varchar(60))");
        students = mock(StudentRepository.class);
        contacts = mock(EmergencyContactRepository.class);
        CurrentUserService current = mock(CurrentUserService.class);
        owner = new User();
        owner.setId(UUID.randomUUID());
        when(current.currentUser()).thenReturn(owner);
        users = mock(UserRepository.class);
        when(users.findByIdForUpdate(owner.getId())).thenReturn(Optional.of(owner));
        when(students.countByUserIdAndDeletedAtIsNull(owner.getId())).thenReturn(0L);
        when(students.save(any(Student.class))).thenAnswer(invocation -> {
            savedStudent = invocation.getArgument(0);
            if (savedStudent.getId() == null) savedStudent.setId(UUID.randomUUID());
            jdbc.update("merge into students (id, name) key(id) values (?, ?)", savedStudent.getId(), savedStudent.getFullName());
            return savedStudent;
        });
        when(contacts.saveAndFlush(any(EmergencyContact.class))).thenAnswer(invocation -> {
            savedContact = invocation.getArgument(0);
            if (savedContact.getId() == null) savedContact.setId(UUID.randomUUID());
            jdbc.update("merge into contacts (id, student_id, name) key(id) values (?, ?, ?)",
                    savedContact.getId(), savedContact.getStudent().getId(), savedContact.getFullName());
            if (failContact) throw new DataIntegrityViolationException("Injected contact persistence failure");
            return savedContact;
        });
        StudentService target = new StudentService(students, contacts, mock(StudentDeviceRepository.class),
                users, mock(RouteRepository.class), mock(StudentRouteRepository.class),
                current, mock(StudentMapper.class), mock(StudentAccessService.class),
                mock(StudentGuardianService.class), mock(StudentDeviceLinkService.class), mock(StudentLinkCodeService.class));
        TransactionInterceptor transactions = new TransactionInterceptor();
        transactions.setTransactionManager(new DataSourceTransactionManager(dataSource));
        transactions.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(transactions);
        service = (StudentService) factory.getProxy();
    }

    private StudentDtos.StudentRequest request(String studentName, String contactName) {
        return new StudentDtos.StudentRequest(null, studentName, SchoolGrade.FIFTH,
                LocalDate.now().minusYears(10), null,
                new EmergencyContactDtos.EmergencyContactRequest(contactName, "3001234567", "Guardian", true));
    }

    @Test
    void commitsStudentAndContactTogether() {
        service.create(request("Ana Perez", "Maria Perez"));
        assertEquals(1, jdbc.queryForObject("select count(*) from students", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from contacts", Integer.class));
        assertEquals(savedStudent.getId(), savedContact.getStudent().getId());
    }

    @Test
    void rejectsStudentCreationWhenGuardianAlreadyHasTwentyActiveChildren() {
        when(students.countByUserIdAndDeletedAtIsNull(owner.getId())).thenReturn(20L);
        assertThrows(IllegalArgumentException.class, () -> service.create(request("Ana Perez", "Maria Perez")));
        assertEquals(0, jdbc.queryForObject("select count(*) from students", Integer.class));
    }

    @Test
    void rejectsStudentsUnderFiveBeforeWritingEitherRecord() {
        var request = new StudentDtos.StudentRequest(null, "Ana Perez", SchoolGrade.PRE_KINDER,
                LocalDate.now().minusYears(4), null,
                new EmergencyContactDtos.EmergencyContactRequest("Maria Perez", "3001234567", "Guardian", true));
        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        assertEquals(0, jdbc.queryForObject("select count(*) from students", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from contacts", Integer.class));
    }

    @Test
    void contactFailureRollsBackBothRecords() {
        failContact = true;
        assertThrows(DataIntegrityViolationException.class, () -> service.create(request("Ana Perez", "Maria Perez")));
        assertEquals(0, jdbc.queryForObject("select count(*) from students", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from contacts", Integer.class));
    }

    @Test
    void updateFailurePreservesBothPreviousValues() {
        service.create(request("Ana Perez", "Maria Perez"));
        UUID id = savedStudent.getId();
        when(students.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(savedStudent));
        when(contacts.findByStudentIdAndIsActiveTrueAndDeletedAtIsNull(id)).thenReturn(List.of(savedContact));
        failContact = true;
        assertThrows(DataIntegrityViolationException.class, () -> service.update(id, request("Ana Gomez", "Maria Gomez")));
        assertEquals("Ana Perez", jdbc.queryForObject("select name from students", String.class));
        assertEquals("Maria Perez", jdbc.queryForObject("select name from contacts", String.class));
    }

    @Test
    void missingContactIsRejectedBeforeAnyWrite() {
        var request = new StudentDtos.StudentRequest(null, "Ana Perez", SchoolGrade.FIFTH,
                LocalDate.now().minusYears(10), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        verify(students, never()).save(any());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("emergencyContact")));
        }
    }

    @Test
    void requestValidationRejectsBlankContactName() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request("Ana Perez", "")).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("emergencyContact.fullName")));
        }
    }

    @Test
    void requestValidationRejectsRepeatedStudentAndContactNames() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var invalidStudent = request("aaa", "Ana Perez");
            var invalidContact = request("Ana Perez", "bbb");
            assertTrue(factory.getValidator().validate(invalidStudent).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("fullName")));
            assertTrue(factory.getValidator().validate(invalidContact).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("emergencyContact.fullName")));
        }
    }
}
