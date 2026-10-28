package com.guardianescolar.api.modules.students.service;

import com.guardianescolar.api.modules.students.domain.EmergencyContact;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.StudentDevice;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import com.guardianescolar.api.modules.students.repository.EmergencyContactRepository;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import com.guardianescolar.api.shared.security.JwtService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentDeviceLinkService {

    private final StudentRepository studentRepository;
    private final StudentDeviceRepository studentDeviceRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final StudentMapper studentMapper;
    private final StudentLinkCodeService studentLinkCodeService;
    private final JwtService jwtService;

    @Transactional
    public StudentDtos.StudentDeviceResponse linkDevice(StudentDtos.LinkDeviceRequest request) {
        Student student = findStudent(request.studentId());
        studentLinkCodeService.consume(student, request.code());

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
        StudentDevice saved = studentDeviceRepository.save(device);
        String childToken = jwtService.generateChildToken(
                student.getUser(), student.getId(), saved.getDeviceIdentifier());
        return studentMapper.toDeviceResponse(saved, childToken);
    }

    public StudentDtos.LinkedProfileResponse linkedProfile(java.util.UUID studentId, String code) {
        Student student = findStudent(studentId);
        studentLinkCodeService.verify(student, code);

        EmergencyContact contact = primaryContact(studentId);
        return new StudentDtos.LinkedProfileResponse(
                student.getId(),
                code,
                student.getFullName(),
                student.getSchoolGrade(),
                student.getBirthDate(),
                contact == null ? "" : contact.getFullName(),
                contact == null ? "" : contact.getPhone(),
                contact == null ? "" : contact.getRelationship(),
                studentDeviceRepository.countByStudentIdAndIsActiveTrueAndDeletedAtIsNull(studentId));
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
        boolean previousStudentStillActive = device.getStudent() != null
                && Boolean.TRUE.equals(device.getStudent().getIsActive())
                && device.getStudent().getDeletedAt() == null;
        if (device.getId() != null
                && device.getStudent() != null
                && !device.getStudent().getId().equals(student.getId())
                && Boolean.TRUE.equals(device.getIsActive())
                && device.getDeletedAt() == null
                && previousStudentStillActive) {
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
private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}


