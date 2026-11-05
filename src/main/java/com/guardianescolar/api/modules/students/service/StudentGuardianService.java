package com.guardianescolar.api.modules.students.service;

import com.guardianescolar.api.modules.auth.service.EmailService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.StudentGuardian;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentGuardianService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentGuardianService.class);

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String RELATIONSHIP_OWNER = "OWNER";
    private static final String RELATIONSHIP_GUARDIAN = "GUARDIAN";
    private static final String RELATIONSHIP_VIEWER = "VIEWER";

    private final UserRepository userRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentMapper studentMapper;
    private final EmailService emailService;

    public StudentDtos.ShareStudentResponse share(Student student, StudentDtos.ShareStudentRequest request) {
        User invitedUser = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("The email does not belong to a registered user"));
        if (invitedUser.getId().equals(student.getUser().getId())) {
            throw new IllegalArgumentException("The owner already has access to this student");
        }

        StudentGuardian access = studentGuardianRepository
                .findByStudentIdAndUserIdAndDeletedAtIsNull(student.getId(), invitedUser.getId())
                .orElseGet(StudentGuardian::new);
        access.setStudent(student);
        access.setUser(invitedUser);
        access.setRelationshipRole(normalizeRelationshipRole(request.relationshipRole()));
        access.setStatus(STATUS_ACTIVE);
        access.setDeletedAt(null);
        StudentGuardian savedAccess = studentGuardianRepository.save(access);
        boolean emailSent = false;
        try {
            emailSent = emailService.enviarNotification(
                    invitedUser.getEmail(),
                    "Acceso de seguimiento compartido - Guardian Escolar",
                    student.getUser().getFullName() + " compartió contigo el seguimiento de "
                            + student.getFullName() + ". Ya puedes ingresar a GPS Guardian Escolar.");
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo enviar el correo de acceso compartido para el estudiante {}", student.getId());
        }
        return new StudentDtos.ShareStudentResponse(studentMapper.toGuardianResponse(savedAccess), emailSent);
    }

    public void revoke(Student student, UUID guardianUserId) {
        if (guardianUserId == null || guardianUserId.equals(student.getUser().getId())) {
            throw new IllegalArgumentException("The student owner access cannot be removed");
        }
        StudentGuardian access = studentGuardianRepository
                .findByStudentIdAndUserIdAndStatusAndDeletedAtIsNull(student.getId(), guardianUserId, STATUS_ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active shared access not found"));
        access.setStatus("REVOKED");
        studentGuardianRepository.save(access);
    }

    public List<StudentDtos.LinkedGuardianResponse> listGuardians(Student student) {
        List<StudentDtos.LinkedGuardianResponse> guardians = new ArrayList<>();
        guardians.add(new StudentDtos.LinkedGuardianResponse(
                null,
                student.getUser().getId(),
                student.getUser().getFullName(),
                student.getUser().getEmail(),
                student.getUser().getPhone(),
                RELATIONSHIP_OWNER,
                STATUS_ACTIVE,
                student.getCreatedAt()));
        guardians.addAll(studentGuardianRepository
                .findByStudentIdAndStatusAndDeletedAtIsNull(student.getId(), STATUS_ACTIVE)
                .stream()
                .map(studentMapper::toGuardianResponse)
                .toList());
        return guardians;
    }

    private String normalizeRelationshipRole(String relationshipRole) {
        if (relationshipRole == null || relationshipRole.isBlank()) {
            return RELATIONSHIP_VIEWER;
        }
        String normalized = relationshipRole.trim().toUpperCase();
        return switch (normalized) {
            case RELATIONSHIP_GUARDIAN, RELATIONSHIP_VIEWER -> normalized;
            default -> RELATIONSHIP_VIEWER;
        };
    }
}
