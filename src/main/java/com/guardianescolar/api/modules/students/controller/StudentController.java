package com.guardianescolar.api.modules.students.controller;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.students.service.StudentService;
import com.guardianescolar.api.modules.students.dto.StudentDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Students", description = "Student records and device linking")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o parámetros no válidos"),
    @ApiResponse(responseCode = "401", description = "Se requiere autenticación"),
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
})
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Listar recursos")
    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ') or hasRole('ADMIN')")
    public List<StudentDtos.StudentResponse> list() {
        return studentService.list();
    }

    @Operation(summary = "Crear recurso")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('STUDENT_CREATE') or hasRole('ADMIN')")
    public StudentDtos.StudentResponse create(@Valid @RequestBody StudentDtos.StudentRequest request) {
        return studentService.create(request);
    }

    @Operation(summary = "Actualizar recurso")
    @PutMapping("/{studentId}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public StudentDtos.StudentResponse update(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.StudentRequest request) {
        return studentService.update(studentId, request);
    }

    @Operation(summary = "Eliminar recurso")
    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_DELETE') or hasRole('ADMIN')")
    public void delete(@PathVariable UUID studentId) {
        studentService.delete(studentId);
    }

    @Operation(summary = "Asignar ruta al estudiante")
    @PostMapping("/{studentId}/routes/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public void assignRoute(@PathVariable UUID studentId, @PathVariable UUID routeId) {
        studentService.assignRoute(studentId, routeId);
    }

    @Operation(summary = "Desvincular dispositivo")
    @DeleteMapping("/{studentId}/device")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public void unlinkDevice(@PathVariable UUID studentId) {
        studentService.unlinkDevice(studentId);
    }

    @Operation(summary = "Listar acudientes autorizados")
    @GetMapping("/{studentId}/guardians")
    @PreAuthorize("hasAuthority('STUDENT_READ') or hasRole('ADMIN')")
    public List<StudentDtos.LinkedGuardianResponse> listGuardians(@PathVariable UUID studentId) {
        return studentService.listGuardians(studentId);
    }

    @Operation(summary = "Compartir estudiante con acudiente")
    @PostMapping("/{studentId}/guardians")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public StudentDtos.ShareStudentResponse share(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.ShareStudentRequest request) {
        return studentService.share(studentId, request);
    }

    @Operation(summary = "Revocar acceso del acudiente")
    @DeleteMapping("/{studentId}/guardians/{guardianUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_UPDATE') or hasRole('ADMIN')")
    public void revokeGuardian(
            @PathVariable UUID studentId,
            @PathVariable UUID guardianUserId) {
        studentService.revokeGuardian(studentId, guardianUserId);
    }

    @Operation(summary = "Vincular dispositivo del estudiante")
    @PostMapping("/link-device")
    public StudentDtos.StudentDeviceResponse linkDevice(
            @Valid @RequestBody StudentDtos.LinkDeviceRequest request) {
        return studentService.linkDevice(request);
    }

    @Operation(summary = "Consultar perfil vinculado")
    @GetMapping("/{studentId}/linked-profile")
    public StudentDtos.LinkedProfileResponse linkedProfile(
            @PathVariable UUID studentId,
            @RequestParam String code) {
        return studentService.linkedProfile(studentId, code);
    }
}
