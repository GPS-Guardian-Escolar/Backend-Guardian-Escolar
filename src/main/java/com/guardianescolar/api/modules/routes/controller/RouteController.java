package com.guardianescolar.api.modules.routes.controller;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.routes.service.RouteService;
import com.guardianescolar.api.modules.routes.dto.RouteDtos;
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
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROUTE_READ') or hasRole('ADMIN')")
    public List<RouteDtos.RouteResponse> list() {
        return routeService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROUTE_CREATE') or hasRole('ADMIN')")
    public RouteDtos.RouteResponse create(@Valid @RequestBody RouteDtos.RouteRequest request) {
        return routeService.create(request);
    }

    @PutMapping("/{routeId}")
    @PreAuthorize("hasAuthority('ROUTE_UPDATE') or hasRole('ADMIN')")
    public RouteDtos.RouteResponse update(
            @PathVariable UUID routeId,
            @Valid @RequestBody RouteDtos.RouteRequest request) {
        return routeService.update(routeId, request);
    }

    @DeleteMapping("/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROUTE_DELETE') or hasRole('ADMIN')")
    public void delete(@PathVariable UUID routeId) {
        routeService.delete(routeId);
    }

    @PostMapping("/{routeId}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROUTE_UPDATE') or hasRole('ADMIN')")
    public RouteDtos.StopResponse addStop(
            @PathVariable UUID routeId,
            @Valid @RequestBody RouteDtos.StopRequest request) {
        return routeService.addStop(routeId, request);
    }
}
