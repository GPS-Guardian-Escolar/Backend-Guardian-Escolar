package com.guardianescolar.api.modules.routes.service;

import com.guardianescolar.api.modules.routes.domain.Stop;
import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.dto.RouteDtos;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public RouteDtos.RouteResponse toResponse(Route route, List<Stop> stops) {
        return new RouteDtos.RouteResponse(
                route.getId(),
                route.getRouteName(),
                route.getDescription(),
                route.getOriginLatitude(),
                route.getOriginLongitude(),
                route.getDestinationLatitude(),
                route.getDestinationLongitude(),
                stops.stream().map(this::toStopResponse).toList());
    }

    public RouteDtos.StopResponse toStopResponse(Stop stop) {
        return new RouteDtos.StopResponse(
                stop.getId(),
                stop.getStopOrder(),
                stop.getStopName(),
                stop.getLatitude(),
                stop.getLongitude());
    }
}
