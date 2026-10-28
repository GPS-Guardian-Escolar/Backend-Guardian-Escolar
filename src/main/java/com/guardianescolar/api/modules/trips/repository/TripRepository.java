package com.guardianescolar.api.modules.trips.repository;

import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.domain.TripStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByDeletedAtIsNullOrderByTripStartedAtDesc();

    List<Trip> findByStudentUserEmailIgnoreCaseAndDeletedAtIsNullOrderByTripStartedAtDesc(String email);

    List<Trip> findByStudentIdInAndDeletedAtIsNullOrderByTripStartedAtDesc(Collection<UUID> studentIds);

    Optional<Trip> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByIdAndStudentUserEmailIgnoreCaseAndDeletedAtIsNull(UUID id, String email);

    long countByStatusAndDeletedAtIsNull(TripStatus status);

    boolean existsByStudentIdAndStatusAndDeletedAtIsNull(UUID studentId, TripStatus status);
}

