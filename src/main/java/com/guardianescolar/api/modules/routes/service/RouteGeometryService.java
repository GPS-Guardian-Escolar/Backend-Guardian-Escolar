package com.guardianescolar.api.modules.routes.service;

import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.domain.Stop;
import com.guardianescolar.api.modules.routes.dto.RouteDtos;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteGeometryService {

    private final OsrmRoutesClient osrmRoutesClient;

    public RouteDtos.RouteGeometryResponse calculate(Route route, List<Stop> stops) {
        OsrmRoutesClient.Geometry geometry = osrmRoutesClient.calculate(
                route.getOriginLatitude(),
                route.getOriginLongitude(),
                stops.stream()
                        .sorted(Comparator.comparing(Stop::getStopOrder))
                        .map(stop -> new OsrmRoutesClient.Intermediate(
                                stop.getLatitude(), stop.getLongitude()))
                        .toList(),
                route.getDestinationLatitude(),
                route.getDestinationLongitude());
        return new RouteDtos.RouteGeometryResponse(
                geometry.encodedPolyline(),
                geometry.distanceMeters(),
                geometry.durationSeconds());
    }
}
