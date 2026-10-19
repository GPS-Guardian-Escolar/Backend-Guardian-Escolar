package com.guardianescolar.api.modules.students.service;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.domain.StudentRoute;
import com.guardianescolar.api.modules.routes.repository.RouteRepository;
import com.guardianescolar.api.modules.routes.repository.StudentRouteRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final int MIN_STUDENT_AGE = 3;
    private static final int MAX_STUDENT_AGE = 21;

    private final StudentRepository studentRepository;
    private final StudentDeviceRepository studentDeviceRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final StudentRouteRepository studentRouteRepository;
    private final CurrentUserService currentUserService;
    private final StudentMapper studentMapper;
    private final StudentAccessService studentAccessService;
    private final StudentGuardianService studentGuardianService;
    private final StudentDeviceLinkService studentDeviceLinkService;

    @Transactional(readOnly = true)
    public List<StudentDtos.StudentResponse> list() {
        User current = currentUserService.currentUser();
        List<Student> students = currentUserService.isAdmin(current)
                ? studentRepository.findByDeletedAtIsNullOrderByFullNameAsc()
                : studentRepository.findByUserEmailIgnoreCaseAndDeletedAtIsNullOrderByFullNameAsc(current.getEmail());
        return studentAccessService.visibleStudents(current, students)
                .stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    @Transactional
    public StudentDtos.StudentResponse create(StudentDtos.StudentRequest request) {
        User current = currentUserService.currentUser();
        Student student = new Student();
        student.setUser(resolveOwner(request.userId(), current));
        applyData(student, request);
        student.setIsActive(true);
        student.setCreatedBy(current);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentDtos.StudentResponse update(UUID studentId, StudentDtos.StudentRequest request) {
        User current = currentUserService.currentUser();
        Student student = getManageable(studentId);
        if (currentUserService.isAdmin(current) && request.userId() != null) {
            student.setUser(findOwner(request.userId()));
        }
        applyData(student, request);
        student.setUpdatedBy(current);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    public void delete(UUID studentId) {
        User current = currentUserService.currentUser();
        Student student = getManageable(studentId);
        student.setIsActive(false);
        student.setDeletedAt(OffsetDateTime.now());
        student.setUpdatedBy(current);
        studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public Student getAllowed(UUID studentId) {
        Student student = findActive(studentId);
        studentAccessService.validateAccess(student);
        return student;
    }

    @Transactional(readOnly = true)
    public Student getManageable(UUID studentId) {
        Student student = findActive(studentId);
        studentAccessService.validateManagement(student);
        return student;
    }

    @Transactional
    public void assignRoute(UUID studentId, UUID routeId) {
        Student student = getManageable(studentId);
        User current = currentUserService.currentUser();
        Route route = routeRepository.findByIdAndDeletedAtIsNull(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        studentAccessService.validateAssignableRoute(route, current);
        if (studentRouteRepository.existsByIdStudentIdAndIdRouteIdAndIsActiveTrue(studentId, routeId)) {
            throw new IllegalArgumentException("The student already has this route assigned");
        }
        studentRouteRepository.save(new StudentRoute(student, route));
    }

    @Transactional
    public void unlinkDevice(UUID studentId) {
        Student student = getManageable(studentId);
        studentDeviceRepository.findFirstByStudentIdAndIsActiveTrueAndDeletedAtIsNull(studentId)
                .ifPresent(device -> {
                    device.setIsActive(false);
                    device.setDeletedAt(OffsetDateTime.now());
                    studentDeviceRepository.save(device);
                });
    }

    @Transactional
    public StudentDtos.LinkedGuardianResponse share(UUID studentId, StudentDtos.ShareStudentRequest request) {
        return studentGuardianService.share(getManageable(studentId), request);
    }

    @Transactional(readOnly = true)
    public List<StudentDtos.LinkedGuardianResponse> listGuardians(UUID studentId) {
        return studentGuardianService.listGuardians(getAllowed(studentId));
    }

    @Transactional
    public StudentDtos.StudentDeviceResponse linkDevice(StudentDtos.LinkDeviceRequest request) {
        return studentDeviceLinkService.linkDevice(request);
    }

    @Transactional(readOnly = true)
    public StudentDtos.LinkedProfileResponse linkedProfile(UUID studentId, String code) {
        return studentDeviceLinkService.linkedProfile(studentId, code);
    }

    private Student findActive(UUID studentId) {
        return studentRepository.findByIdAndDeletedAtIsNull(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private void applyData(Student student, StudentDtos.StudentRequest request) {
        validateBirthDate(request.birthDate());
        student.setFullName(request.fullName().trim());
        student.setSchoolGrade(request.schoolGrade());
        student.setBirthDate(request.birthDate());
        if (request.photoData() != null) {
            student.setPhotoData(request.photoData());
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date is required");
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < MIN_STUDENT_AGE || age > MAX_STUDENT_AGE) {
            throw new IllegalArgumentException(
                    "The student's age must be between " + MIN_STUDENT_AGE + " and " + MAX_STUDENT_AGE + " years");
        }
    }

    private User resolveOwner(UUID userId, User current) {
        if (userId == null || !currentUserService.isAdmin(current)) {
            return current;
        }
        return findOwner(userId);
    }

    private User findOwner(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found"));
    }
}
