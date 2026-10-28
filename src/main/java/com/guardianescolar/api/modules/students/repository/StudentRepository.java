package com.guardianescolar.api.modules.students.repository;

import com.guardianescolar.api.modules.students.domain.Student;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    List<Student> findByDeletedAtIsNullOrderByFullNameAsc();

    List<Student> findByUserEmailIgnoreCaseAndDeletedAtIsNullOrderByFullNameAsc(String email);

    Optional<Student> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUserIdAndFullNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String fullName);

    boolean existsByUserIdAndFullNameIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID userId, String fullName, UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = """
            UPDATE students
            SET link_code_failed_attempts = COALESCE(link_code_failed_attempts, 0) + 1
            WHERE id = :studentId AND COALESCE(link_code_failed_attempts, 0) < 5
            """, nativeQuery = true)
    int incrementLinkCodeFailedAttempts(@Param("studentId") UUID studentId);
}
