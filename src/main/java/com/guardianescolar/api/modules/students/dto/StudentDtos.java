package com.guardianescolar.api.modules.students.dto;

import com.guardianescolar.api.modules.students.domain.SchoolGrade;
import com.guardianescolar.api.modules.routes.dto.RouteDtos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class StudentDtos {

    private StudentDtos() {
    }

    public record StudentRequest(
            UUID userId,
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @NotNull SchoolGrade schoolGrade,
            @NotNull @Past LocalDate birthDate,
            @Size(max = 1500000, message = "La imagen del estudiante es demasiado grande") String photoData) {
    }

    public record StudentResponse(
            UUID id,
            UUID userId,
            String guardianName,
            String guardianEmail,
            String fullName,
            SchoolGrade schoolGrade,
            LocalDate birthDate,
            String photoData,
            Boolean isActive,
            Long linkedDevices,
            Long linkedGuardians,
            List<RouteDtos.RouteResponse> routes) {
    }

    public record ShareStudentRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @Size(max = 30) String relationshipRole) {
    }

    public record LinkedGuardianResponse(
            UUID id,
            UUID userId,
            String fullName,
            String email,
            String phone,
            String relationshipRole,
            String status,
            OffsetDateTime createdAt) {
    }

    public record LinkDeviceRequest(
            UUID studentId,
            @NotBlank @Size(min = 6, max = 20) String code,
            @NotBlank @Size(min = 8, max = 160) String deviceIdentifier,
            @NotBlank @Pattern(regexp = "ANDROID|IOS|WEB", message = "platform debe ser ANDROID, IOS o WEB") String platform,
            @Size(max = 120) String deviceName) {
    }

    public record StudentDeviceResponse(
            UUID id,
            UUID studentId,
            String platform,
            String deviceName,
            Boolean isActive,
            OffsetDateTime linkedAt,
            OffsetDateTime lastUsedAt,
            Long linkedDevices) {
    }

    public record LinkedProfileResponse(
            UUID id,
            String code,
            String fullName,
            SchoolGrade schoolGrade,
            LocalDate birthDate,
            String contactName,
            String contactPhone,
            String contactRelationship) {
    }
}
