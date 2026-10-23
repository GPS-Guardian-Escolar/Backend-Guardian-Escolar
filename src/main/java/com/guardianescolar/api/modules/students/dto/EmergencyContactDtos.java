package com.guardianescolar.api.modules.students.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class EmergencyContactDtos {

    private EmergencyContactDtos() {
    }

    public record EmergencyContactRequest(
            @NotBlank @Size(min = 2, max = 80) String fullName,
            @NotBlank @Pattern(regexp = "^3\\d{9}$", message = "teléfono debe ser un celular colombiano válido de 10 dígitos") String phone,
            @Size(max = 50) String relationship,
            Boolean isPrimary) {
    }

    public record EmergencyContactResponse(
            UUID id,
            UUID studentId,
            String fullName,
            String phone,
            String relationship,
            Boolean isPrimary,
            Boolean isActive) {
    }
}
