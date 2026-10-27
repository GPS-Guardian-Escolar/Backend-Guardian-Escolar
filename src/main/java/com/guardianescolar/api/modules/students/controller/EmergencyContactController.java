package com.guardianescolar.api.modules.students.controller;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.students.service.EmergencyContactService;
import com.guardianescolar.api.modules.students.dto.EmergencyContactDtos;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ') or hasRole('ADMIN')")
    public List<EmergencyContactDtos.EmergencyContactResponse> list(@PathVariable UUID studentId) {
        return emergencyContactService.list(studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public EmergencyContactDtos.EmergencyContactResponse create(
            @PathVariable UUID studentId,
            @Valid @RequestBody EmergencyContactDtos.EmergencyContactRequest request) {
        return emergencyContactService.create(studentId, request);
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public EmergencyContactDtos.EmergencyContactResponse update(
            @PathVariable UUID studentId,
            @PathVariable UUID contactId,
            @Valid @RequestBody EmergencyContactDtos.EmergencyContactRequest request) {
        return emergencyContactService.update(studentId, contactId, request);
    }

    @DeleteMapping("/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public void delete(@PathVariable UUID studentId, @PathVariable UUID contactId) {
        emergencyContactService.delete(studentId, contactId);
    }
}
