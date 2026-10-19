package com.guardianescolar.api.modules.routes.service;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.domain.Stop;
import com.guardianescolar.api.modules.routes.dto.RouteDtos;
import com.guardianescolar.api.modules.routes.repository.RouteRepository;
import com.guardianescolar.api.modules.routes.repository.StopRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final CurrentUserService currentUserService;
    private final RouteAccessService routeAccessService;
    private final RouteValidationService routeValidationService;
    private final RouteMapper routeMapper;

    @Transactional(readOnly = true)
    public List<RouteDtos.RouteResponse> list() {
        User current = currentUserService.currentUser();
        List<Route> routes = currentUserService.isAdmin(current)
                ? routeRepository.findByDeletedAtIsNullOrderByRouteNameAsc()
                : routeRepository.findByCreatedByIdAndDeletedAtIsNullOrderByRouteNameAsc(current.getId());
        return routes.stream()
                .map(route -> routeMapper.toResponse(route, stopsFor(route)))
                .toList();
    }

    @Transactional
    public RouteDtos.RouteResponse create(RouteDtos.RouteRequest request) {
        User current = currentUserService.currentUser();
        routeValidationService.validateRequest(request);
        Route route = new Route();
        applyData(route, request);
        route.setCreatedBy(current);
        Route saved = routeRepository.save(route);
        return routeMapper.toResponse(saved, saveStops(saved, request.stops()));
    }

    @Transactional
    public RouteDtos.RouteResponse update(UUID routeId, RouteDtos.RouteRequest request) {
        User current = currentUserService.currentUser();
        Route route = findActive(routeId);
        routeAccessService.validateAccess(route, current);
        routeValidationService.validateRequest(request);
        applyData(route, request);
        route.setUpdatedBy(current);
        Route saved = routeRepository.save(route);
        return routeMapper.toResponse(saved, replaceStopsIfRequested(saved, request.stops()));
    }

    @Transactional
    public void delete(UUID routeId) {
        User current = currentUserService.currentUser();
        Route route = findActive(routeId);
        routeAccessService.validateAccess(route, current);
        route.setDeletedAt(OffsetDateTime.now());
        route.setUpdatedBy(current);
        routeRepository.save(route);
    }

    @Transactional
    public RouteDtos.StopResponse addStop(UUID routeId, RouteDtos.StopRequest request) {
        User current = currentUserService.currentUser();
        Route route = findActive(routeId);
        routeAccessService.validateAccess(route, current);
        Stop stop = routeValidationService.createStop(route, request);
        return routeMapper.toStopResponse(stopRepository.save(stop));
    }

    @Transactional(readOnly = true)
    public Route getAllowed(UUID routeId) {
        Route route = findActive(routeId);
        routeAccessService.validateAccess(route, currentUserService.currentUser());
        return route;
    }

    private Route findActive(UUID routeId) {
        return routeRepository.findByIdAndDeletedAtIsNull(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
    }

    private void applyData(Route route, RouteDtos.RouteRequest request) {
        route.setRouteName(request.routeName().trim());
        route.setDescription(routeValidationService.normalizeOptionalText(request.description()));
        route.setOriginLatitude(request.originLatitude());
        route.setOriginLongitude(request.originLongitude());
        route.setDestinationLatitude(request.destinationLatitude());
        route.setDestinationLongitude(request.destinationLongitude());
    }

    private List<Stop> replaceStopsIfRequested(Route route, List<RouteDtos.StopRequest> requests) {
        if (requests == null) {
            return stopsFor(route);
        }
        stopRepository.deleteByRouteId(route.getId());
        return saveStops(route, requests);
    }

    private List<Stop> saveStops(Route route, List<RouteDtos.StopRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<Stop> stops = requests.stream()
                .map(request -> routeValidationService.createStop(route, request))
                .toList();
        return stopRepository.saveAll(stops).stream()
                .sorted(Comparator.comparing(Stop::getStopOrder))
                .toList();
    }

    private List<Stop> stopsFor(Route route) {
        return stopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
    }
}
