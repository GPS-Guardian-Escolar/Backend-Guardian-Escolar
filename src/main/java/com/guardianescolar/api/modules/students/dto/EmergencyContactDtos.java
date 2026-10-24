package com.guardianescolar.api.modules.students.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class EmergencyContactDtos {

    private EmergencyContactDtos() {
    }

    public record EmergencyContactRequest(
            @NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "^(?!.*(.)\\1{2})[\\p{L}\\p{M}]+(?:[ '\\u2019-][\\p{L}\\p{M}]+)*$", message = "El nombre solo puede contener letras y no repetir caracteres de forma excesiva") String fullName,
            @NotBlank @Pattern(regexp = "^(?!.*(\\d)\\1{4})3\\d{9}$", message = "El celular debe tener 10 dígitos, comenzar por 3 y no repetir cinco o más dígitos seguidos") String phone,
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
