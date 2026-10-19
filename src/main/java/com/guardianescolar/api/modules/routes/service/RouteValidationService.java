package com.guardianescolar.api.modules.routes.service;

import com.guardianescolar.api.modules.routes.domain.Stop;
import com.guardianescolar.api.modules.routes.dto.RouteDtos;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RouteValidationService {

    public void validateRequest(RouteDtos.RouteRequest request) {
        if (request.originLatitude() == null || request.originLongitude() == null
                || request.destinationLatitude() == null || request.destinationLongitude() == null) {
            throw new IllegalArgumentException("Origin and destination must be geocoded before saving the route");
        }
        validateCoordinatePair("origin", request.originLatitude(), request.originLongitude());
        validateCoordinatePair("destination", request.destinationLatitude(), request.destinationLongitude());
        if (request.stops() == null || request.stops().isEmpty()) {
            return;
        }
        Set<Integer> orders = new HashSet<>();
        for (RouteDtos.StopRequest stop : request.stops()) {
            if (!orders.add(stop.stopOrder())) {
                throw new IllegalArgumentException("Stops cannot repeat the same order");
            }
        }
    }

    public String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Stop createStop(com.guardianescolar.api.modules.routes.domain.Route route, RouteDtos.StopRequest request) {
        Stop stop = new Stop();
        stop.setRoute(route);
        stop.setStopOrder(request.stopOrder());
        stop.setStopName(request.stopName().trim());
        stop.setLatitude(request.latitude());
        stop.setLongitude(request.longitude());
        return stop;
    }

    private void validateCoordinatePair(String name, BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("The " + name + " coordinate must include latitude and longitude");
        }
    }
}
