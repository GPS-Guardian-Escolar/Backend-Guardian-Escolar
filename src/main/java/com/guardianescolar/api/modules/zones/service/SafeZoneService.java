package com.guardianescolar.api.modules.zones.service;

import com.guardianescolar.api.modules.auth.service.CurrentUserService;
import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.service.StudentService;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.zones.domain.SafeZone;
import com.guardianescolar.api.modules.zones.dto.SafeZoneDtos;
import com.guardianescolar.api.modules.zones.repository.SafeZoneRepository;
import com.guardianescolar.api.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SafeZoneService {

    private static final List<String> VALID_ZONE_TYPES = List.of("Casa", "Escuela", "Personalizado");

    private final SafeZoneRepository safeZoneRepository;
    private final StudentService studentService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<SafeZoneDtos.SafeZoneResponse> list() {
        User current = currentUserService.currentUser();
        List<SafeZone> zones = currentUserService.isAdmin(current)
                ? safeZoneRepository.findByIsActiveTrueAndDeletedAtIsNullOrderByZoneNameAsc()
                : safeZoneRepository.findByStudentUserEmailIgnoreCaseAndIsActiveTrueAndDeletedAtIsNullOrderByZoneNameAsc(current.getEmail());
        return zones.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SafeZoneDtos.SafeZoneResponse create(SafeZoneDtos.SafeZoneRequest request) {
        User current = currentUserService.currentUser();
        Student student = studentService.getManageable(request.studentId());
        SafeZone zone = new SafeZone();
        applyData(zone, request, student);
        zone.setIsActive(true);
        zone.setCreatedBy(current);
        return toResponse(safeZoneRepository.save(zone));
    }

    @Transactional
    public SafeZoneDtos.SafeZoneResponse update(UUID zoneId, SafeZoneDtos.SafeZoneRequest request) {
        User current = currentUserService.currentUser();
        SafeZone zone = getAllowed(zoneId, current);
        Student student = studentService.getManageable(request.studentId());
        applyData(zone, request, student);
        zone.setUpdatedBy(current);
        return toResponse(safeZoneRepository.save(zone));
    }

    @Transactional
    public void delete(UUID zoneId) {
        User current = currentUserService.currentUser();
        SafeZone zone = getAllowed(zoneId, current);
        zone.setIsActive(false);
        zone.setDeletedAt(OffsetDateTime.now());
        zone.setUpdatedBy(current);
        safeZoneRepository.save(zone);
    }

    private SafeZone getAllowed(UUID zoneId, User current) {
        SafeZone zone = safeZoneRepository.findByIdAndDeletedAtIsNull(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Safe zone not found"));
        boolean owner = zone.getStudent().getUser().getId().equals(current.getId());
        if (!currentUserService.isAdmin(current) && !owner) {
            throw new AccessDeniedException("You do not have access to this safe zone");
        }
        return zone;
    }

    private void applyData(SafeZone zone, SafeZoneDtos.SafeZoneRequest request, Student student) {
        zone.setStudent(student);
        zone.setZoneName(request.zoneName().trim());
        zone.setZoneType(VALID_ZONE_TYPES.contains(request.zoneType()) ? request.zoneType() : "Personalizado");
        zone.setAddress(request.address().trim());
        zone.setLatitude(request.latitude());
        zone.setLongitude(request.longitude());
        zone.setRadiusMeters(request.radiusMeters());
        zone.setInactivityAlertSeconds(
                request.inactivityAlertSeconds() == null ? 300 : request.inactivityAlertSeconds());
    }

    private SafeZoneDtos.SafeZoneResponse toResponse(SafeZone zone) {
        return new SafeZoneDtos.SafeZoneResponse(
                zone.getId(),
                zone.getStudent().getId(),
                zone.getStudent().getFullName(),
                zone.getZoneName(),
                zone.getZoneType(),
                zone.getAddress(),
                zone.getLatitude(),
                zone.getLongitude(),
                zone.getRadiusMeters(),
                zone.getInactivityAlertSeconds(),
                zone.getIsActive());
    }
}
