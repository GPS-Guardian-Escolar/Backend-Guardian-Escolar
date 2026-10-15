package com.guardianescolar.api.modules.trips.repository;

import com.guardianescolar.api.modules.trips.domain.Coordinate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinateRepository extends JpaRepository<Coordinate, UUID> {

    List<Coordinate> findByTripIdOrderByRecordedAtAsc(UUID TripId);

    List<Coordinate> findTop3ByTripIdOrderByRecordedAtDesc(UUID TripId);
}
