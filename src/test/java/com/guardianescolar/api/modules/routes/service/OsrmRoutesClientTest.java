package com.guardianescolar.api.modules.routes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OsrmRoutesClientTest {

    @Test
    void calculatesGeometryWithOsrmResponse() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(
                        "http://localhost/route/v1/driving/-74.1,4.5;-74.2,4.7;-74.3,4.9?overview=full&geometries=polyline&steps=false"))
                .andRespond(withSuccess(
                        "{\"routes\":[{\"geometry\":\"abc123\",\"distance\":1200,\"duration\":90}],\"code\":\"Ok\"}",
                        MediaType.APPLICATION_JSON));

        OsrmRoutesClient client = new OsrmRoutesClient(restClientBuilder, "http://localhost");
        OsrmRoutesClient.Geometry geometry = client.calculate(
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(-74.1),
                List.of(new OsrmRoutesClient.Intermediate(BigDecimal.valueOf(4.7), BigDecimal.valueOf(-74.2))),
                BigDecimal.valueOf(4.9),
                BigDecimal.valueOf(-74.3));

        assertEquals("abc123", geometry.encodedPolyline());
        assertEquals(1200, geometry.distanceMeters());
        assertEquals(90L, geometry.durationSeconds());
        server.verify();
    }

    @Test
    void ordersLongitudeBeforeLatitudeWhenBuildingCoordinates() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(
                        "http://localhost/route/v1/driving/-74.1,4.5;-74.2,4.7;-74.3,4.9?overview=full&geometries=polyline&steps=false"))
                .andRespond(withSuccess(
                        "{\"routes\":[{\"geometry\":\"abc123\",\"distance\":1200,\"duration\":90}],\"code\":\"Ok\"}",
                        MediaType.APPLICATION_JSON));

        OsrmRoutesClient client = new OsrmRoutesClient(restClientBuilder, "http://localhost");
        client.calculate(
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(-74.1),
                List.of(new OsrmRoutesClient.Intermediate(BigDecimal.valueOf(4.7), BigDecimal.valueOf(-74.2))),
                BigDecimal.valueOf(4.9),
                BigDecimal.valueOf(-74.3));

        server.verify();
    }

    @Test
    void failsWhenOsrmReportsNoRoute() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(
                        "http://localhost/route/v1/driving/-74.1,4.5;-74.3,4.9?overview=full&geometries=polyline&steps=false"))
                .andRespond(withSuccess(
                        "{\"code\":\"NoRoute\",\"message\":\"No route found\",\"routes\":[]}",
                        MediaType.APPLICATION_JSON));

        OsrmRoutesClient client = new OsrmRoutesClient(restClientBuilder, "http://localhost");
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.calculate(
                        BigDecimal.valueOf(4.5),
                        BigDecimal.valueOf(-74.1),
                        List.of(),
                        BigDecimal.valueOf(4.9),
                        BigDecimal.valueOf(-74.3)));

        assertTrue(exception.getMessage().contains("NoRoute"));
        server.verify();
    }

    @Test
    void failsWhenOsrmHttpRequestFails() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(
                        "http://localhost/route/v1/driving/-74.1,4.5;-74.3,4.9?overview=full&geometries=polyline&steps=false"))
                .andRespond(withServerError().body("bad gateway"));

        OsrmRoutesClient client = new OsrmRoutesClient(restClientBuilder, "http://localhost");
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.calculate(
                        BigDecimal.valueOf(4.5),
                        BigDecimal.valueOf(-74.1),
                        List.of(),
                        BigDecimal.valueOf(4.9),
                        BigDecimal.valueOf(-74.3)));

        assertTrue(exception.getMessage().contains("HTTP error"));
        server.verify();
    }
}
