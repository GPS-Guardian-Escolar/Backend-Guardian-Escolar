package com.guardianescolar.api.modules.students.repository;

import com.guardianescolar.api.modules.students.domain.StudentDevice;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentDeviceRepository extends JpaRepository<StudentDevice, UUID> {

    long countByStudentIdAndIsActiveTrueAndDeletedAtIsNull(UUID studentId);

    Optional<StudentDevice> findByDeviceIdentifier(String deviceIdentifier);

    Optional<StudentDevice> findFirstByStudentIdAndIsActiveTrueAndDeletedAtIsNull(UUID studentId);

    List<StudentDevice> findByStudentIdAndIsActiveTrueAndDeletedAtIsNull(UUID studentId);
}
