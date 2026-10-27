package com.guardianescolar.api.modules.zones.controller;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.zones.service.SafeZoneService;
import com.guardianescolar.api.modules.zones.dto.SafeZoneDtos;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/safe-zones")
@RequiredArgsConstructor
@Tag(name = "Safe zones", description = "Safe-zone configuration and alerts")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o parámetros no válidos"),
    @ApiResponse(responseCode = "401", description = "Se requiere autenticación"),
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
})
public class SafeZoneController {

    private final SafeZoneService safeZoneService;

    @Operation(summary = "Listar recursos")
    @GetMapping
    @PreAuthorize("hasAuthority('SAFE_ZONE_READ') or hasRole('ADMIN')")
    public List<SafeZoneDtos.SafeZoneResponse> list() {
        return safeZoneService.list();
    }

    @Operation(summary = "Crear recurso")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SAFE_ZONE_CREATE') or hasRole('ADMIN')")
    public SafeZoneDtos.SafeZoneResponse create(@Valid @RequestBody SafeZoneDtos.SafeZoneRequest request) {
        return safeZoneService.create(request);
    }

    @Operation(summary = "Actualizar recurso")
    @PutMapping("/{zoneId}")
    @PreAuthorize("hasAuthority('SAFE_ZONE_UPDATE') or hasRole('ADMIN')")
    public SafeZoneDtos.SafeZoneResponse update(
            @PathVariable UUID zoneId,
            @Valid @RequestBody SafeZoneDtos.SafeZoneRequest request) {
        return safeZoneService.update(zoneId, request);
    }

    @Operation(summary = "Eliminar recurso")
    @DeleteMapping("/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SAFE_ZONE_DELETE') or hasRole('ADMIN')")
    public void delete(@PathVariable UUID zoneId) {
        safeZoneService.delete(zoneId);
    }
}
