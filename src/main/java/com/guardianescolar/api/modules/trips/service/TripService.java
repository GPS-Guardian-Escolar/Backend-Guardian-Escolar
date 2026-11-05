package com.guardianescolar.api.modules.trips.service;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.notifications.domain.NotificationEventTypes;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import com.guardianescolar.api.modules.students.service.StudentAccessService;
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
import com.guardianescolar.api.shared.Pagination;
import com.guardianescolar.api.shared.dto.PageResponse;
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

    private static final double MOVEMENT_START_THRESHOLD_METERS = 40.0;

    private final com.guardianescolar.api.modules.students.repository.StudentDeviceRepository studentDeviceRepository;

    private final TripRepository tripRepository;
    private final CoordinateRepository coordinateRepository;
    private final StudentRouteRepository studentRouteRepository;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final StudentAccessService studentAccessService;
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
        List<Trip> trips;
        if (currentUserService.isAdmin(current)) {
            trips = tripRepository.findByDeletedAtIsNullOrderByTripStartedAtDesc();
        } else {
            List<Student> ownedStudents = studentRepository
                    .findByUserEmailIgnoreCaseAndDeletedAtIsNullOrderByFullNameAsc(current.getEmail());
            List<UUID> visibleStudentIds = studentAccessService.visibleStudents(current, ownedStudents).stream()
                    .map(Student::getId)
                    .toList();
            trips = visibleStudentIds.isEmpty()
                    ? List.of()
                    : tripRepository.findByStudentIdInAndDeletedAtIsNullOrderByTripStartedAtDesc(visibleStudentIds);
        }
        return trips.stream().map(tripMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TripDtos.TripResponse> listPage(int page, int size) {
        return Pagination.slice(list(), page, size);
    }

    @Transactional
    public TripDtos.TripResponse create(TripDtos.TripRequest request) {
        // Creating a tracking session only arms GPS monitoring. A trip is
        // started and notified after registerCoordinate confirms real movement.
        return createMonitoring(request);
    }

    @Transactional
    public TripDtos.TripResponse createMonitoring(TripDtos.TripRequest request) {
        validateChildStudent(request.studentId());
        Student student = studentService.getAllowed(request.studentId());
        validateTripMutation(student);
        var inProgress = tripRepository.findFirstByStudentIdAndStatusAndDeletedAtIsNullOrderByTripStartedAtDesc(
                student.getId(), TripStatus.IN_PROGRESS);
        if (inProgress.isPresent()) return tripMapper.toResponse(inProgress.get());
        var pending = tripRepository.findFirstByStudentIdAndStatusAndDeletedAtIsNullOrderByTripStartedAtDesc(
                student.getId(), TripStatus.PENDING);
        if (pending.isPresent()) return tripMapper.toResponse(pending.get());

        UUID selectedRouteId = request.routeId() == null
                ? studentRouteRepository.findFirstByIdStudentIdAndIsActiveTrueAndIsPreferredTrue(student.getId())
                        .map(assignment -> assignment.getRoute())
                        .filter(candidate -> candidate.getDeletedAt() == null)
                        .map(Route::getId)
                        .orElseGet(() -> {
                            List<com.guardianescolar.api.modules.routes.domain.StudentRoute> assigned = studentRouteRepository.findByIdStudentIdAndIsActiveTrue(student.getId());
                            List<Route> available = assigned.stream().map(com.guardianescolar.api.modules.routes.domain.StudentRoute::getRoute)
                                    .filter(candidate -> candidate.getDeletedAt() == null).toList();
                            if (available.size() == 1) return available.get(0).getId();
                            throw new IllegalArgumentException("The guardian must select a route before starting the next trip");
                        })
                : request.routeId();
        Route route = routeService.getAllowed(selectedRouteId);
        if (!studentRouteRepository.existsByIdStudentIdAndIdRouteIdAndIsActiveTrue(student.getId(), route.getId())) {
            throw new IllegalArgumentException("The student does not have the requested route assigned");
        }

        Trip monitoring = new Trip();
        monitoring.setStudent(student);
        monitoring.setRoute(route);
        monitoring.setTripStartedAt(OffsetDateTime.now());
        monitoring.setStatus(TripStatus.PENDING);
        monitoring.setHadDeviation(false);
        return tripMapper.toResponse(tripRepository.save(monitoring));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<TripDtos.TripResponse> activeForStudent(UUID studentId) {
        validateChildStudent(studentId);
        studentService.getAllowed(studentId);
        return tripRepository.findFirstByStudentIdAndStatusAndDeletedAtIsNullOrderByTripStartedAtDesc(
                studentId, TripStatus.IN_PROGRESS)
                .or(() -> tripRepository.findFirstByStudentIdAndStatusAndDeletedAtIsNullOrderByTripStartedAtDesc(
                        studentId, TripStatus.PENDING))
                .map(tripMapper::toResponse);
    }

    @Transactional
    public void delete(UUID tripId) {
        Trip trip = getAllowed(tripId);
        studentAccessService.validateManagement(trip.getStudent());
        if (trip.getStatus() == TripStatus.IN_PROGRESS) {
            trip.setStatus(TripStatus.CANCELED);
            trip.setTripEndedAt(OffsetDateTime.now());
        }
        trip.setDeletedAt(OffsetDateTime.now());
        tripRepository.save(trip);
    }

    @Transactional
    public TripDtos.CoordinateResponse registerCoordinate(UUID tripId, TripDtos.CoordinateRequest request) {
        Trip trip = getAllowed(tripId);
        validateTripMutation(trip.getStudent());
        if (trip.getStatus() != TripStatus.IN_PROGRESS && trip.getStatus() != TripStatus.PENDING) {
            throw new IllegalArgumentException("Coordinates require an active trip");
        }
        Coordinate coordinate = new Coordinate();
        coordinate.setTrip(trip);
        coordinate.setLatitude(request.latitude());
        coordinate.setLongitude(request.longitude());
        coordinate.setRecordedAt(request.recordedAt() == null ? OffsetDateTime.now() : request.recordedAt());
        coordinate.setStoppedSeconds(request.stoppedSeconds() == null ? 0 : request.stoppedSeconds());
        List<Coordinate> recentCoordinates = coordinateRepository.findTop3ByTripIdOrderByRecordedAtDesc(tripId);
        Coordinate previous = recentCoordinates.stream().findFirst().orElse(null);
        Coordinate beforePrevious = recentCoordinates.size() > 1 ? recentCoordinates.get(1) : null;
        if (previous != null && !coordinate.getRecordedAt().isAfter(previous.getRecordedAt())) {
            return tripMapper.toCoordinateResponse(previous);
        }
        if (previous != null && isImpossibleJump(previous, beforePrevious, coordinate)) {
            return tripMapper.toCoordinateResponse(coordinate);
        }
        Coordinate origin = trip.getStatus() == TripStatus.PENDING
                ? coordinateRepository.findFirstByTripIdOrderByRecordedAtAsc(tripId)
                : null;
        double distanceFromOrigin = origin == null ? 0 : geoMathService.distanceMeters(
                origin.getLatitude(), origin.getLongitude(), coordinate.getLatitude(), coordinate.getLongitude());
        double previousDistanceFromOrigin = origin == null || previous == null ? 0 : geoMathService.distanceMeters(
                origin.getLatitude(), origin.getLongitude(), previous.getLatitude(), previous.getLongitude());
        Coordinate saved;
        boolean startsTrip = trip.getStatus() == TripStatus.PENDING && origin != null && previous != null
                && distanceFromOrigin >= MOVEMENT_START_THRESHOLD_METERS
                && previousDistanceFromOrigin >= MOVEMENT_START_THRESHOLD_METERS * 0.75
                && distanceFromOrigin - previousDistanceFromOrigin >= 5.0
                && geoMathService.distanceMeters(previous.getLatitude(), previous.getLongitude(),
                        coordinate.getLatitude(), coordinate.getLongitude()) >= 5.0;
        if (startsTrip) {
            saved = coordinateRepository.save(coordinate);
            trip.setStatus(TripStatus.IN_PROGRESS);
            trip.setTripStartedAt(saved.getRecordedAt());
            tripRepository.save(trip);
            notificationService.createForUser(trip, trip.getStudent().getUser(),
                    NotificationEventTypes.TRIP_STARTED,
                    "Se iniciÃ³ el trayecto de " + trip.getStudent().getFullName());
        } else if (trip.getStatus() == TripStatus.PENDING && origin != null && previous != null
                && !origin.getId().equals(previous.getId())) {
            // Keep a fixed departure anchor and one fresh point while waiting,
            // so a stationary phone does not fill the trip history indefinitely.
            previous.setLatitude(coordinate.getLatitude());
            previous.setLongitude(coordinate.getLongitude());
            previous.setRecordedAt(coordinate.getRecordedAt());
            previous.setStoppedSeconds(coordinate.getStoppedSeconds());
            saved = coordinateRepository.save(previous);
        } else {
            saved = coordinateRepository.save(coordinate);
        }
        TripDtos.CoordinateResponse response = tripMapper.toCoordinateResponse(saved);
        if (trip.getStatus() == TripStatus.IN_PROGRESS) tripAlertService.processCoordinate(trip, saved);
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

    @Transactional(readOnly = true)
    public PageResponse<TripDtos.CoordinateResponse> listCoordinatesPage(UUID tripId, int page, int size) {
        return Pagination.slice(listCoordinates(tripId), page, size);
    }

    private Trip getAllowed(UUID tripId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        validateChildStudent(trip.getStudent().getId());
        studentAccessService.validateAccess(trip.getStudent());
        return trip;
    }

    private void validateTripMutation(Student student) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails()
                instanceof com.guardianescolar.api.shared.security.ChildDeviceAuthenticationDetails) {
            validateChildStudent(student.getId());
            return;
        }
        studentAccessService.validateManagement(student);
    }

    private void validateChildStudent(UUID studentId) {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof com.guardianescolar.api.shared.security.ChildDeviceAuthenticationDetails child) {
            boolean linked = child.studentId().equals(studentId) && studentDeviceRepository
                    .findByDeviceIdentifier(child.deviceIdentifier())
                    .filter(device -> Boolean.TRUE.equals(device.getIsActive()) && device.getDeletedAt() == null)
                    .filter(device -> device.getStudent() != null && studentId.equals(device.getStudent().getId()))
                    .isPresent();
            if (!linked) throw new AccessDeniedException("The device is not linked to this student");
        }
    }
}
