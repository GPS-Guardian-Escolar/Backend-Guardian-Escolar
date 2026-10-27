package com.guardianescolar.api.modules.routes.controller;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.routes.service.RouteService;
import com.guardianescolar.api.modules.routes.dto.RouteDtos;
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
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description = "School routes and stops")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o parámetros no válidos"),
    @ApiResponse(responseCode = "401", description = "Se requiere autenticación"),
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
})
public class RouteController {

    private final RouteService routeService;

    @Operation(summary = "Listar recursos")
    @GetMapping
    @PreAuthorize("hasAuthority('ROUTE_READ') or hasRole('ADMIN')")
    public List<RouteDtos.RouteResponse> list() {
        return routeService.list();
    }

    @Operation(summary = "Crear recurso")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROUTE_CREATE') or hasRole('ADMIN')")
    public RouteDtos.RouteResponse create(@Valid @RequestBody RouteDtos.RouteRequest request) {
        return routeService.create(request);
    }

    @Operation(summary = "Actualizar recurso")
    @PutMapping("/{routeId}")
    @PreAuthorize("hasAuthority('ROUTE_UPDATE') or hasRole('ADMIN')")
    public RouteDtos.RouteResponse update(
            @PathVariable UUID routeId,
            @Valid @RequestBody RouteDtos.RouteRequest request) {
        return routeService.update(routeId, request);
    }

    @Operation(summary = "Eliminar recurso")
    @DeleteMapping("/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROUTE_DELETE') or hasRole('ADMIN')")
    public void delete(@PathVariable UUID routeId) {
        routeService.delete(routeId);
    }

    @Operation(summary = "Agregar parada a una ruta")
    @PostMapping("/{routeId}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROUTE_UPDATE') or hasRole('ADMIN')")
    public RouteDtos.StopResponse addStop(
            @PathVariable UUID routeId,
            @Valid @RequestBody RouteDtos.StopRequest request) {
        return routeService.addStop(routeId, request);
    }
}
