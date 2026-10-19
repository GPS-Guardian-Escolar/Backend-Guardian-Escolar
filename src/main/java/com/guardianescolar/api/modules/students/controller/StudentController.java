package com.guardianescolar.api.modules.students.controller;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.students.service.StudentService;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ') or hasRole('ADMIN')")
    public List<StudentDtos.StudentResponse> list() {
        return studentService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('STUDENT_CREATE') or hasRole('ADMIN')")
    public StudentDtos.StudentResponse create(@Valid @RequestBody StudentDtos.StudentRequest request) {
        return studentService.create(request);
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public StudentDtos.StudentResponse update(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.StudentRequest request) {
        return studentService.update(studentId, request);
    }

    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_DELETE') or hasRole('ADMIN')")
    public void delete(@PathVariable UUID studentId) {
        studentService.delete(studentId);
    }

    @PostMapping("/{studentId}/routes/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public void assignRoute(@PathVariable UUID studentId, @PathVariable UUID routeId) {
        studentService.assignRoute(studentId, routeId);
    }

    @DeleteMapping("/{studentId}/device")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public void unlinkDevice(@PathVariable UUID studentId) {
        studentService.unlinkDevice(studentId);
    }

    @GetMapping("/{studentId}/guardians")
    @PreAuthorize("hasAuthority('STUDENT_READ') or hasRole('ADMIN')")
    public List<StudentDtos.LinkedGuardianResponse> listGuardians(@PathVariable UUID studentId) {
        return studentService.listGuardians(studentId);
    }

    @PostMapping("/{studentId}/guardians")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public StudentDtos.LinkedGuardianResponse share(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.ShareStudentRequest request) {
        return studentService.share(studentId, request);
    }

    @PostMapping("/link-device")
    public StudentDtos.StudentDeviceResponse linkDevice(
            @Valid @RequestBody StudentDtos.LinkDeviceRequest request) {
        return studentService.linkDevice(request);
    }

    @GetMapping("/{studentId}/linked-profile")
    public StudentDtos.LinkedProfileResponse linkedProfile(
            @PathVariable UUID studentId,
            @RequestParam String code) {
        return studentService.linkedProfile(studentId, code);
    }
}
