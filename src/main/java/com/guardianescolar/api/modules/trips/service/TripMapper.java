package com.guardianescolar.api.modules.trips.service;

import com.guardianescolar.api.modules.trips.domain.Coordinate;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.dto.TripDtos;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    public TripDtos.TripResponse toResponse(Trip trip) {
        return new TripDtos.TripResponse(
                trip.getId(),
                trip.getStudent().getId(),
                trip.getStudent().getFullName(),
                trip.getRoute().getId(),
                trip.getRoute().getRouteName(),
                trip.getTripStartedAt(),
                trip.getTripEndedAt(),
                trip.getStatus(),
                trip.getHadDeviation());
    }

    public TripDtos.CoordinateResponse toCoordinateResponse(Coordinate coordinate) {
        return new TripDtos.CoordinateResponse(
                coordinate.getId(),
                coordinate.getTrip().getId(),
                coordinate.getLatitude(),
                coordinate.getLongitude(),
                coordinate.getRecordedAt(),
                coordinate.getStoppedSeconds());
    }
}
