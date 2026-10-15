package com.guardianescolar.api.modules.trips.controller;

import lombok.RequiredArgsConstructor;

import com.guardianescolar.api.modules.trips.service.TripService;
import com.guardianescolar.api.modules.trips.dto.TripDtos;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasRole('ADMIN')")
    public List<TripDtos.TripResponse> list() {
        return tripService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasAuthority('CHILD_DEVICE') or hasRole('ADMIN')")
    public TripDtos.TripResponse create(@Valid @RequestBody TripDtos.TripRequest request) {
        return tripService.create(request);
    }

    @PatchMapping("/{tripId}/status")
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasRole('ADMIN')")
    public TripDtos.TripResponse changeStatus(
            @PathVariable UUID tripId,
            @Valid @RequestBody TripDtos.ChangeStatusRequest request) {
        return tripService.changeStatus(tripId, request);
    }

    @PostMapping("/{tripId}/coordinates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasAuthority('CHILD_DEVICE') or hasRole('ADMIN')")
    public TripDtos.CoordinateResponse registerCoordinate(
            @PathVariable UUID tripId,
            @Valid @RequestBody TripDtos.CoordinateRequest request) {
        return tripService.registerCoordinate(tripId, request);
    }

    @GetMapping("/{tripId}/coordinates")
    @PreAuthorize("hasAuthority('LOCATION_VIEW') or hasRole('ADMIN')")
    public List<TripDtos.CoordinateResponse> listCoordinates(@PathVariable UUID tripId) {
        return tripService.listCoordinates(tripId);
    }
}
