package com.guardianescolar.api.modules.students.repository;

import com.guardianescolar.api.modules.students.domain.StudentGuardian;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, UUID> {

    List<StudentGuardian> findByUserIdAndStatusAndDeletedAtIsNull(UUID UserId, String status);

    List<StudentGuardian> findByStudentIdAndStatusAndDeletedAtIsNull(UUID studentId, String status);

    Optional<StudentGuardian> findByStudentIdAndUserIdAndDeletedAtIsNull(UUID studentId, UUID userId);

    Optional<StudentGuardian> findByStudentIdAndUserIdAndStatusAndDeletedAtIsNull(
            UUID studentId, UUID userId, String status);

    boolean existsByStudentIdAndUserIdAndStatusAndDeletedAtIsNull(UUID studentId, UUID userId, String status);

    long countByStudentIdAndStatusAndDeletedAtIsNull(UUID studentId, String status);
}
