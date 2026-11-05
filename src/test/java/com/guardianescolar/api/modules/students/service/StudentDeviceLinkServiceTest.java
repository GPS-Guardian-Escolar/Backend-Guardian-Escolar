package com.guardianescolar.api.modules.students.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.domain.StudentDevice;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import com.guardianescolar.api.modules.students.repository.EmergencyContactRepository;
import com.guardianescolar.api.modules.students.repository.StudentDeviceRepository;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;
import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.shared.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudentDeviceLinkServiceTest {

    @Test
    void notifiesGuardianWhenNewDeviceIsLinkedButDoesNotCallItAnActiveTrip() {
        StudentRepository students = org.mockito.Mockito.mock(StudentRepository.class);
        StudentDeviceRepository devices = org.mockito.Mockito.mock(StudentDeviceRepository.class);
        EmergencyContactRepository contacts = org.mockito.Mockito.mock(EmergencyContactRepository.class);
        StudentGuardianRepository guardians = org.mockito.Mockito.mock(StudentGuardianRepository.class);
        StudentMapper mapper = org.mockito.Mockito.mock(StudentMapper.class);
        StudentLinkCodeService codes = org.mockito.Mockito.mock(StudentLinkCodeService.class);
        JwtService jwt = org.mockito.Mockito.mock(JwtService.class);
        NotificationService notifications = org.mockito.Mockito.mock(NotificationService.class);

        Student student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Jose Maria");
        User guardian = new User();
        guardian.setId(UUID.randomUUID());
        student.setUser(guardian);
        StudentDevice device = new StudentDevice();
        device.setId(UUID.randomUUID());
        StudentDtos.StudentDeviceResponse response = new StudentDtos.StudentDeviceResponse(
                device.getId(), student.getId(), "ANDROID", "Phone", true, null, null, 1L, "child-token");

        when(students.findByIdAndDeletedAtIsNull(student.getId())).thenReturn(Optional.of(student));
        when(devices.findByDeviceIdentifier("student-device-12345678")).thenReturn(Optional.empty());
        when(devices.findFirstByStudentIdAndIsActiveTrueAndDeletedAtIsNull(student.getId())).thenReturn(Optional.empty());
        when(devices.save(any(StudentDevice.class))).thenReturn(device);
        when(guardians.findByStudentIdAndStatusAndDeletedAtIsNull(student.getId(), "ACTIVE")).thenReturn(List.of());
        when(jwt.generateChildToken(guardian, student.getId(), "student-device-12345678")).thenReturn("child-token");
        when(mapper.toDeviceResponse(device, "child-token")).thenReturn(response);

        StudentDeviceLinkService service = new StudentDeviceLinkService(students, devices, contacts, mapper, codes, jwt,
                guardians, notifications);
        service.linkDevice(new StudentDtos.LinkDeviceRequest(
                student.getId(), "ABCDEFGH12345678", "student-device-12345678", "ANDROID", "Phone"));

        verify(notifications, times(1)).createForUser(isNull(), eq(guardian),
                eq(NotificationEventTypes.DEVICE_LINKED), org.mockito.ArgumentMatchers.contains("iniciará al detectar movimiento"));
    }

    @Test
    void retriesACodeAlreadyConsumedByTheSameLinkedDevice() {
        StudentRepository students = org.mockito.Mockito.mock(StudentRepository.class);
        StudentDeviceRepository devices = org.mockito.Mockito.mock(StudentDeviceRepository.class);
        StudentMapper mapper = org.mockito.Mockito.mock(StudentMapper.class);
        StudentLinkCodeService codes = org.mockito.Mockito.mock(StudentLinkCodeService.class);
        JwtService jwt = org.mockito.Mockito.mock(JwtService.class);

        Student student = new Student();
        student.setId(UUID.randomUUID());
        User guardian = new User();
        guardian.setId(UUID.randomUUID());
        student.setUser(guardian);
        StudentDevice device = new StudentDevice();
        device.setId(UUID.randomUUID());
        device.setStudent(student);
        device.setDeviceIdentifier("student-device-12345678");
        device.setIsActive(true);
        StudentDtos.StudentDeviceResponse response = new StudentDtos.StudentDeviceResponse(
                device.getId(), student.getId(), "WEB", "Phone", true, null, null, 1L, "child-token");

        when(students.findByIdAndDeletedAtIsNull(student.getId())).thenReturn(Optional.of(student));
        when(devices.findByDeviceIdentifier(device.getDeviceIdentifier())).thenReturn(Optional.of(device));
        when(devices.save(device)).thenReturn(device);
        when(jwt.generateChildToken(guardian, student.getId(), device.getDeviceIdentifier())).thenReturn("child-token");
        when(mapper.toDeviceResponse(device, "child-token")).thenReturn(response);

        StudentDeviceLinkService service = new StudentDeviceLinkService(students, devices,
                org.mockito.Mockito.mock(EmergencyContactRepository.class), mapper, codes, jwt,
                org.mockito.Mockito.mock(StudentGuardianRepository.class),
                org.mockito.Mockito.mock(NotificationService.class));

        StudentDtos.StudentDeviceResponse linked = service.linkDevice(new StudentDtos.LinkDeviceRequest(
                student.getId(), "ABCDEFGH12345678", device.getDeviceIdentifier(), "WEB", "Phone"));

        assertEquals("child-token", linked.childToken());
        verify(codes).verifyConsumedForSameDevice(student, "ABCDEFGH12345678");
        verify(codes, never()).consume(any(), any());
        verify(devices).save(device);
    }
}
