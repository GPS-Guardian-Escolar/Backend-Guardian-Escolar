package com.guardianescolar.api.modules.trips.service;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.service.StudentService;
import com.guardianescolar.api.modules.notifications.service.NotificationService;
import com.guardianescolar.api.modules.routes.domain.Route;
import com.guardianescolar.api.modules.routes.repository.StudentRouteRepository;
import com.guardianescolar.api.modules.routes.service.RouteService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.trips.domain.Coordinate;
import com.guardianescolar.api.modules.trips.domain.TripStatus;
import com.guardianescolar.api.modules.trips.domain.Trip;
import com.guardianescolar.api.modules.trips.dto.TripDtos;
import com.guardianescolar.api.modules.trips.repository.CoordinateRepository;
import com.guardianescolar.api.modules.trips.repository.TripRepository;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final CoordinateRepository coordinateRepository;
    private final StudentRouteRepository studentRouteRepository;
    private final StudentService studentService;
    private final RouteService routeService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final TripAlertService tripAlertService;
    private final TripMapper tripMapper;
    private final GeoMathService geoMathService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<TripDtos.TripResponse> list() {
        User current = currentUserService.currentUser();
        List<Trip> trips = currentUserService.isAdmin(current)
                ? tripRepository.findByDeletedAtIsNullOrderByTripStartedAtDesc()
                : tripRepository.findByStudentUserEmailIgnoreCaseAndDeletedAtIsNullOrderByTripStartedAtDesc(current.getEmail());
        return trips.stream().map(tripMapper::toResponse).toList();
    }

    @Transactional
    public TripDtos.TripResponse create(TripDtos.TripRequest request) {
        validateChildStudent(request.studentId());
        Student student = studentService.getAllowed(request.studentId());
        Route route = request.routeId() == null
                ? studentRouteRepository.findByIdStudentIdAndIsActiveTrue(request.studentId()).stream()
                        .map(com.guardianescolar.api.modules.routes.domain.StudentRoute::getRoute)
                        .filter(candidate -> candidate.getDeletedAt() == null)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("The student has no active route assigned"))
                : routeService.getAllowed(request.routeId());
        if (!studentRouteRepository.existsByIdStudentIdAndIdRouteIdAndIsActiveTrue(student.getId(), route.getId())) {
            throw new IllegalArgumentException("The student does not have the requested route assigned");
        }

        Trip trip = new Trip();
        trip.setStudent(student);
        trip.setRoute(route);
        trip.setTripStartedAt(request.tripStartedAt() == null ? OffsetDateTime.now() : request.tripStartedAt());
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setHadDeviation(false);
        Trip saved = tripRepository.save(trip);
        notificationService.createForUser(
                saved,
                student.getUser(),
                NotificationEventTypes.TRIP_STARTED,
                "Se inició el trayecto de " + student.getFullName());
        return tripMapper.toResponse(saved);
    }

    @Transactional
    public TripDtos.TripResponse changeStatus(UUID tripId, TripDtos.ChangeStatusRequest request) {
        Trip trip = getAllowed(tripId);
        trip.setStatus(request.status());
        if (request.hadDeviation() != null) {
            trip.setHadDeviation(request.hadDeviation());
        }
        if (request.status() == TripStatus.COMPLETED || request.status() == TripStatus.CANCELED) {
            trip.setTripEndedAt(OffsetDateTime.now());
        }
        return tripMapper.toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripDtos.CoordinateResponse registerCoordinate(UUID tripId, TripDtos.CoordinateRequest request) {
        Trip trip = getAllowed(tripId);
        Coordinate coordinate = new Coordinate();
        coordinate.setTrip(trip);
        coordinate.setLatitude(request.latitude());
        coordinate.setLongitude(request.longitude());
        coordinate.setRecordedAt(request.recordedAt() == null ? OffsetDateTime.now() : request.recordedAt());
        coordinate.setSpeedKmh(request.speedKmh());
        coordinate.setStoppedSeconds(request.stoppedSeconds() == null ? 0 : request.stoppedSeconds());
        List<Coordinate> recentCoordinates = coordinateRepository.findTop3ByTripIdOrderByRecordedAtDesc(tripId);
        Coordinate previous = recentCoordinates.stream().findFirst().orElse(null);
        Coordinate beforePrevious = recentCoordinates.size() > 1 ? recentCoordinates.get(1) : null;
        if (previous != null && isImpossibleJump(previous, beforePrevious, coordinate)) {
            return tripMapper.toCoordinateResponse(coordinate);
        }
        Coordinate saved = coordinateRepository.save(coordinate);
        TripDtos.CoordinateResponse response = tripMapper.toCoordinateResponse(saved);

        tripAlertService.processCoordinate(trip, saved);
        messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/coordinates", response);
        return response;
    }

    private boolean isImpossibleJump(Coordinate previous, Coordinate beforePrevious, Coordinate current) {
        double distance = geoMathService.distanceMeters(
                previous.getLatitude(), previous.getLongitude(), current.getLatitude(), current.getLongitude());
        long elapsedSeconds = Math.max(1, Math.abs(Duration.between(previous.getRecordedAt(), current.getRecordedAt()).getSeconds()));
        boolean returnsToPrevious = beforePrevious != null
                && distance > 300
                && geoMathService.distanceMeters(
                        beforePrevious.getLatitude(), beforePrevious.getLongitude(), current.getLatitude(), current.getLongitude()) < 300;
        return returnsToPrevious || (distance > 1000 && distance / elapsedSeconds > 30);
    }

    @Transactional(readOnly = true)
    public List<TripDtos.CoordinateResponse> listCoordinates(UUID tripId) {
        getAllowed(tripId);
        return coordinateRepository.findByTripIdOrderByRecordedAtAsc(tripId).stream()
                .map(tripMapper::toCoordinateResponse)
                .toList();
    }

    private Trip getAllowed(UUID tripId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        User current = currentUserService.currentUser();
        validateChildStudent(trip.getStudent().getId());
        boolean owner = trip.getStudent().getUser().getId().equals(current.getId());
        if (!currentUserService.isAdmin(current) && !owner) {
            throw new AccessDeniedException("You do not have access to this trip");
        }
        return trip;
    }

    private void validateChildStudent(UUID studentId) {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof com.guardianescolar.api.shared.security.ChildDeviceAuthenticationDetails child
                && !child.studentId().equals(studentId)) {
            throw new AccessDeniedException("The device is not linked to this student");
        }
    }
}
