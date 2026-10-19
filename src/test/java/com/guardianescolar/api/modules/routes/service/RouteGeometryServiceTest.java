package com.guardianescolar.api.modules.routes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.guardianescolar.api.modules.routes.domain.Route;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteGeometryServiceTest {

    @Mock
    private OsrmRoutesClient osrmRoutesClient;

    @Test
    void returnsTransientGeometryWithoutPersistingRoute() {
        when(osrmRoutesClient.calculate(
                latitude(), longitude(), List.of(), latitude(), longitude()))
                .thenReturn(new OsrmRoutesClient.Geometry("polyline", 1200, 90));

        Route route = route();
        var response = new RouteGeometryService(osrmRoutesClient).calculate(route, List.of());

        assertEquals("polyline", response.encodedPolyline());
        assertEquals(1200, response.distanceMeters());
        assertEquals(90L, response.estimatedDurationSeconds());
        assertThrows(NoSuchFieldException.class, () -> Route.class.getDeclaredField("encodedPolyline"));
    }

    private static Route route() {
        Route route = new Route();
        route.setOriginLatitude(latitude());
        route.setOriginLongitude(longitude());
        route.setDestinationLatitude(latitude());
        route.setDestinationLongitude(longitude());
        return route;
    }

    private static BigDecimal latitude() {
        return BigDecimal.valueOf(4.5);
    }

    private static BigDecimal longitude() {
        return BigDecimal.valueOf(-74.1);
    }
}
