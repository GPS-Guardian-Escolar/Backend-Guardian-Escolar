package com.guardianescolar.api.modules.students.service;

import com.guardianescolar.api.modules.students.domain.EmergencyContact;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.StudentDevice;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import com.guardianescolar.api.modules.students.repository.EmergencyContactRepository;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentDeviceLinkService {

    private final StudentRepository studentRepository;
    private final StudentDeviceRepository studentDeviceRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final StudentMapper studentMapper;
    private final StudentLinkCodeService studentLinkCodeService;

    public StudentDtos.StudentDeviceResponse linkDevice(StudentDtos.LinkDeviceRequest request) {
        Student student = request.studentId() == null
                ? findStudentByCode(request.code())
                : findStudent(request.studentId());
        if (!studentLinkCodeService.matches(student.getId(), request.code())) {
            throw new IllegalArgumentException("Invalid link code");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String deviceIdentifier = request.deviceIdentifier().trim();
        StudentDevice device = resolveLinkableDevice(student, deviceIdentifier);
        device.setStudent(student);
        device.setDeviceIdentifier(deviceIdentifier);
        device.setPlatform(request.platform());
        device.setDeviceName(normalizeOptionalText(request.deviceName()));
        device.setIsActive(true);
        if (device.getLinkedAt() == null) {
            device.setLinkedAt(now);
        }
        device.setLastUsedAt(now);
        device.setDeletedAt(null);
        return studentMapper.toDeviceResponse(studentDeviceRepository.save(device));
    }

    public StudentDtos.LinkedProfileResponse linkedProfile(java.util.UUID studentId, String code) {
        Student student = findStudent(studentId);
        if (!studentLinkCodeService.matches(student.getId(), code)) {
            throw new IllegalArgumentException("Invalid link code");
        }

        EmergencyContact contact = primaryContact(studentId);
        return new StudentDtos.LinkedProfileResponse(
                student.getId(),
                studentLinkCodeService.codeFor(student.getId()),
                student.getFullName(),
                student.getSchoolGrade(),
                student.getBirthDate(),
                contact == null ? "" : contact.getFullName(),
                contact == null ? "" : contact.getPhone(),
                contact == null ? "" : contact.getRelationship());
    }

    private StudentDevice resolveLinkableDevice(Student student, String deviceIdentifier) {
        StudentDevice activeDevice = studentDeviceRepository
                .findFirstByStudentIdAndIsActiveTrueAndDeletedAtIsNull(student.getId())
                .orElse(null);
        if (activeDevice != null && !activeDevice.getDeviceIdentifier().equals(deviceIdentifier)) {
            throw new IllegalArgumentException("This student already has a linked phone");
        }

        StudentDevice device = studentDeviceRepository.findByDeviceIdentifier(deviceIdentifier)
                .orElseGet(StudentDevice::new);
        if (device.getId() != null
                && !device.getStudent().getId().equals(student.getId())
                && Boolean.TRUE.equals(device.getIsActive())
                && device.getDeletedAt() == null) {
            throw new IllegalArgumentException("This phone is already linked to another student");
        }
        return device;
    }

    private EmergencyContact primaryContact(java.util.UUID studentId) {
        List<EmergencyContact> contacts = emergencyContactRepository
                .findByStudentIdAndIsActiveTrueAndDeletedAtIsNull(studentId);
        return contacts.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsPrimary()))
                .findFirst()
                .orElseGet(() -> contacts.stream().findFirst().orElse(null));
    }

    private Student findStudent(java.util.UUID studentId) {
        return studentRepository.findByIdAndDeletedAtIsNull(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private Student findStudentByCode(String code) {
        return studentRepository.findByDeletedAtIsNullOrderByFullNameAsc().stream()
                .filter(candidate -> studentLinkCodeService.matches(candidate.getId(), code))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for link code"));
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
