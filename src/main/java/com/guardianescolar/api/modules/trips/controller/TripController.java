package com.guardianescolar.api.modules.trips.controller;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.trips.service.TripService;
import com.guardianescolar.api.modules.trips.dto.TripDtos;
import com.guardianescolar.api.shared.Pagination;
import com.guardianescolar.api.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "GPS trips, coordinates and status")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o parámetros no válidos"),
    @ApiResponse(responseCode = "401", description = "Se requiere autenticación"),
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
})
public class TripController {

    private final TripService tripService;

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasAuthority('CHILD_DEVICE') or hasRole('ADMIN')")
    public org.springframework.http.ResponseEntity<TripDtos.TripResponse> active(@RequestParam UUID studentId) {
        return tripService.activeForStudent(studentId)
                .map(org.springframework.http.ResponseEntity::ok)
                .orElseGet(() -> org.springframework.http.ResponseEntity.noContent().build());
    }

    @Operation(summary = "Listar recursos")
    @GetMapping
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasRole('ADMIN')")
    public PageResponse<TripDtos.TripResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tripService.listPage(page, Pagination.size(size));
    }

    @Operation(summary = "Crear recurso")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasAuthority('CHILD_DEVICE') or hasRole('ADMIN')")
    public TripDtos.TripResponse create(@Valid @RequestBody TripDtos.TripRequest request) {
        return tripService.create(request);
    }

    @Operation(summary = "Iniciar seguimiento antes del movimiento")
    @PostMapping("/monitoring")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasAuthority('CHILD_DEVICE') or hasRole('ADMIN')")
    public TripDtos.TripResponse createMonitoring(@Valid @RequestBody TripDtos.TripRequest request) {
        return tripService.createMonitoring(request);
    }

    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasRole('ADMIN')")
    public void delete(@PathVariable UUID tripId) {
        tripService.delete(tripId);
    }

    @Operation(summary = "Registrar coordenada del trayecto")
    @PostMapping("/{tripId}/coordinates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasAuthority('CHILD_DEVICE') or hasRole('ADMIN')")
    public TripDtos.CoordinateResponse registerCoordinate(
            @PathVariable UUID tripId,
            @Valid @RequestBody TripDtos.CoordinateRequest request) {
        return tripService.registerCoordinate(tripId, request);
    }

    @Operation(summary = "Listar coordenadas del trayecto")
    @GetMapping("/{tripId}/coordinates")
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasRole('ADMIN')")
    public PageResponse<TripDtos.CoordinateResponse> listCoordinates(
            @PathVariable UUID tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tripService.listCoordinatesPage(tripId, page, Pagination.size(size));
    }
}
