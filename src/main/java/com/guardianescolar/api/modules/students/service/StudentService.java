package com.guardianescolar.api.modules.students.service;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.domain.StudentRoute;
import com.guardianescolar.api.modules.routes.repository.RouteRepository;
import com.guardianescolar.api.modules.routes.repository.StudentRouteRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.EmergencyContact;
import com.guardianescolar.api.modules.students.dto.EmergencyContactDtos;
import com.guardianescolar.api.modules.students.repository.EmergencyContactRepository;
import com.guardianescolar.api.modules.students.domain.SchoolGrade;
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

    private static final int MIN_STUDENT_AGE = 5;
    private static final int MAX_STUDENT_AGE = 18;
    private static final int MAX_ACTIVE_CHILDREN_PER_GUARDIAN = 20;

    private final StudentRepository studentRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final StudentDeviceRepository studentDeviceRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final StudentRouteRepository studentRouteRepository;
    private final CurrentUserService currentUserService;
    private final StudentMapper studentMapper;
    private final StudentAccessService studentAccessService;
    private final StudentGuardianService studentGuardianService;
    private final StudentDeviceLinkService studentDeviceLinkService;
    private final StudentLinkCodeService studentLinkCodeService;

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
        requireEmergencyContact(request);
        User current = currentUserService.currentUser();
        Student student = new Student();
        User owner = resolveOwner(request.userId(), current);
        owner = userRepository.findByIdForUpdate(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found"));
        if (studentRepository.countByUserIdAndDeletedAtIsNull(owner.getId()) >= MAX_ACTIVE_CHILDREN_PER_GUARDIAN) {
            throw new IllegalArgumentException("This account has reached the limit of "
                    + MAX_ACTIVE_CHILDREN_PER_GUARDIAN + " active students");
        }
        validateUniqueName(owner, request.fullName(), null);
        student.setUser(owner);
        applyData(student, request);
        student.setIsActive(true);
        student.setCreatedBy(current);
        Student saved = studentRepository.save(student);
        savePrimaryContact(saved, request.emergencyContact(), current, true);
        return studentMapper.toResponse(saved);
    }

    @Transactional
    public StudentDtos.StudentResponse update(UUID studentId, StudentDtos.StudentRequest request) {
        requireEmergencyContact(request);
        User current = currentUserService.currentUser();
        Student student = getManageable(studentId);
        if (currentUserService.isAdmin(current) && request.userId() != null) {
            student.setUser(findOwner(request.userId()));
        }
        validateUniqueName(student.getUser(), request.fullName(), student.getId());
        applyData(student, request);
        student.setUpdatedBy(current);
        Student saved = studentRepository.save(student);
        savePrimaryContact(saved, request.emergencyContact(), current, false);
        return studentMapper.toResponse(saved);
    }

    private void requireEmergencyContact(StudentDtos.StudentRequest request) {
        if (request.emergencyContact() == null) {
            throw new IllegalArgumentException("El contacto de emergencia es obligatorio");
        }
    }

    private void savePrimaryContact(Student student, EmergencyContactDtos.EmergencyContactRequest request,
            User current, boolean creating) {
        List<EmergencyContact> contacts = creating ? List.of()
                : emergencyContactRepository.findByStudentIdAndIsActiveTrueAndDeletedAtIsNull(student.getId());
        EmergencyContact contact = contacts.stream().filter(item -> Boolean.TRUE.equals(item.getIsPrimary()))
                .findFirst().orElseGet(() -> contacts.isEmpty() ? new EmergencyContact() : contacts.get(0));
        if (contact.getId() == null) {
            contact.setStudent(student);
            contact.setCreatedBy(current);
        } else {
            contact.setUpdatedBy(current);
        }
        contact.setFullName(request.fullName().trim());
        contact.setPhone(request.phone());
        contact.setRelationship(request.relationship());
        contact.setIsPrimary(true);
        contact.setIsActive(true);
        // Flush both writes inside this transaction so a contact failure rolls back the student too.
        emergencyContactRepository.saveAndFlush(contact);
    }

    @Transactional
    public void delete(UUID studentId) {
        User current = currentUserService.currentUser();
        Student student = getManageable(studentId);
        student.setIsActive(false);
        student.setDeletedAt(OffsetDateTime.now());
        student.setUpdatedBy(current);
        studentRepository.save(student);
        unlinkActiveDevices(studentId);
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
        getManageable(studentId);
        unlinkActiveDevices(studentId);
    }

    @Transactional
    public StudentDtos.ShareStudentResponse share(UUID studentId, StudentDtos.ShareStudentRequest request) {
        return studentGuardianService.share(getManageable(studentId), request);
    }

    @Transactional
    public void revokeGuardian(UUID studentId, UUID guardianUserId) {
        studentGuardianService.revoke(getManageable(studentId), guardianUserId);
    }

    @Transactional(readOnly = true)
    public List<StudentDtos.LinkedGuardianResponse> listGuardians(UUID studentId) {
        return studentGuardianService.listGuardians(getAllowed(studentId));
    }

    @Transactional
    public StudentDtos.LinkCodeResponse issueLinkCode(UUID studentId) {
        Student student = getManageable(studentId);
        StudentLinkCodeService.IssuedCode issued = studentLinkCodeService.issue(student);
        return new StudentDtos.LinkCodeResponse(issued.value(), issued.expiresAt());
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
        validateBirthDate(request.birthDate(), request.schoolGrade());
        student.setFullName(request.fullName().trim());
        student.setSchoolGrade(request.schoolGrade());
        student.setBirthDate(request.birthDate());
        if (request.photoData() != null) {
            student.setPhotoData(request.photoData());
        }
    }

    private void validateBirthDate(LocalDate birthDate, SchoolGrade schoolGrade) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date is required");
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < MIN_STUDENT_AGE || age > MAX_STUDENT_AGE) {
            throw new IllegalArgumentException(
                    "The student's age must be between " + MIN_STUDENT_AGE + " and " + MAX_STUDENT_AGE + " years");
        }
        int[] range = ageRangeForSchoolGrade(schoolGrade);
        if (age < range[0] || age > range[1]) {
            throw new IllegalArgumentException("The student's age does not match the selected school grade");
        }
    }

    private void unlinkActiveDevices(UUID studentId) {
        List<com.guardianescolar.api.modules.students.domain.StudentDevice> devices =
                studentDeviceRepository.findByStudentIdAndIsActiveTrueAndDeletedAtIsNull(studentId);
        if (devices.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        devices.forEach(device -> {
            device.setIsActive(false);
            device.setDeletedAt(now);
        });
        studentDeviceRepository.saveAll(devices);
    }

    private int[] ageRangeForSchoolGrade(SchoolGrade grade) {
        return switch (grade) {
            case PRE_KINDER -> new int[] { 5, 5 };
            case KINDER -> new int[] { 5, 6 };
            case TRANSITION -> new int[] { 5, 7 };
            case FIRST -> new int[] { 6, 8 };
            case SECOND -> new int[] { 7, 9 };
            case THIRD -> new int[] { 8, 10 };
            case FOURTH -> new int[] { 9, 11 };
            case FIFTH -> new int[] { 10, 12 };
            case SIXTH -> new int[] { 11, 13 };
            case SEVENTH -> new int[] { 12, 14 };
            case EIGHTH -> new int[] { 13, 15 };
            case NINTH -> new int[] { 14, 16 };
            case TENTH -> new int[] { 15, 17 };
            case ELEVENTH -> new int[] { 16, 18 };
        };
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

    private void validateUniqueName(User owner, String fullName, UUID excludedStudentId) {
        String normalizedName = fullName.trim().replaceAll("\\s+", " ");
        boolean exists = excludedStudentId == null
                ? studentRepository.existsByUserIdAndFullNameIgnoreCaseAndDeletedAtIsNull(owner.getId(), normalizedName)
                : studentRepository.existsByUserIdAndFullNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
                        owner.getId(), normalizedName, excludedStudentId);
        if (exists) {
            throw new IllegalArgumentException("Ya existe un estudiante con ese nombre en esta cuenta");
        }
    }
}
